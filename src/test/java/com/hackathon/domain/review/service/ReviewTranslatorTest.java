package com.hackathon.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.domain.llm.exception.LlmException;
import com.hackathon.domain.llm.prompt.LlmPrompt;
import com.hackathon.domain.llm.prompt.PromptRegistry;
import com.hackathon.domain.llm.service.LlmClient;
import com.hackathon.domain.review.dto.response.ReviewTranslationBatch;
import com.hackathon.global.common.Language;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ReviewTranslatorTest {

    PromptRegistry promptRegistry = mock(PromptRegistry.class);
    LlmClient llmClient = mock(LlmClient.class);
    JsonMapper jsonMapper = JsonMapper.builder().build();
    ReviewTranslator translator = new ReviewTranslator(promptRegistry, llmClient, jsonMapper);

    @BeforeEach
    void setUp() {
        when(promptRegistry.resolve(eq("translate-review"), any()))
                .thenReturn(new LlmPrompt("translate-review", "model", null, 100, "system"));
    }

    @SuppressWarnings("unchecked")
    private void respondWith(Function<Integer, List<String>> byCount) {
        when(llmClient.entity(any(), anyString(), eq(ReviewTranslationBatch.class), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    int count = jsonMapper.readTree((String) invocation.getArgument(1)).get("texts").size();
                    return new ReviewTranslationBatch(byCount.apply(count));
                });
    }

    @Test
    void 많으면_나눠서_부르고_순서를_지킨다() {
        respondWith(count -> IntStream.range(0, count).mapToObj(i -> "t").toList());
        List<String> texts = IntStream.range(0, ReviewTranslator.MAX_BATCH * 2 + 5).mapToObj(i -> "글" + i).toList();

        assertThat(translator.translate(texts, Language.ZH)).hasSize(texts.size());
        verify(llmClient, times(3)).entity(any(), anyString(), eq(ReviewTranslationBatch.class), any());
    }

    @Test
    void 빈_목록이면_부르지_않는다() {
        assertThat(translator.translate(List.of(), Language.EN)).isEmpty();
        verify(llmClient, never()).entity(any(), anyString(), any(), any());
    }

    @Test
    void 개수가_다르면_LLM_INVALID_OUTPUT() {
        respondWith(count -> List.of("하나만"));

        assertThatThrownBy(() -> translator.translate(List.of("a", "b"), Language.EN))
                .isInstanceOf(LlmException.class)
                .extracting(e -> ((LlmException) e).getErrorCode().getCode())
                .isEqualTo("LLM_INVALID_OUTPUT");
    }
}
