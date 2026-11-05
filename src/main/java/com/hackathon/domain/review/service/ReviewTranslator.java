package com.hackathon.domain.review.service;

import com.hackathon.domain.llm.exception.LlmErrorType;
import com.hackathon.domain.llm.exception.LlmException;
import com.hackathon.domain.llm.prompt.LlmPrompt;
import com.hackathon.domain.llm.prompt.PromptRegistry;
import com.hackathon.domain.llm.service.LlmClient;
import com.hackathon.domain.review.dto.response.ReviewTranslationBatch;
import com.hackathon.global.common.Language;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * 한걸음 여러 개를 한 번의 AI 호출로 번역한다 (translate-review). 저장은 ReviewService가 한다.
 * F4 현장 해설 번역(translate-explanation)과는 프롬프트를 따로 쓴다 (팀 규칙).
 */
@Component
@RequiredArgsConstructor
class ReviewTranslator {

    private static final String PROMPT_KEY = "translate-review";
    /** 한 번의 AI 호출에 넣는 후기 수. 목록 한 페이지(기본 20개)가 한 번에 들어간다 */
    static final int MAX_BATCH = 30;

    private final PromptRegistry promptRegistry;
    private final LlmClient llmClient;
    private final JsonMapper jsonMapper;

    /** 같은 순서·같은 개수로 돌려준다. MAX_BATCH개씩 나눠 부르고, 개수가 맞지 않으면 502 LLM_INVALID_OUTPUT */
    List<String> translate(List<String> texts, Language target) {
        if (texts.isEmpty()) return List.of();
        LlmPrompt prompt = promptRegistry.resolve(PROMPT_KEY, Map.of("language", target.getKoreanName()));
        List<String> result = new ArrayList<>(texts.size());
        for (int from = 0; from < texts.size(); from += MAX_BATCH) {
            result.addAll(call(prompt, texts.subList(from, Math.min(from + MAX_BATCH, texts.size())), target));
        }
        return result;
    }

    private List<String> call(LlmPrompt prompt, List<String> sources, Language target) {
        String input = jsonMapper.writeValueAsString(Map.of("texts", sources));
        ReviewTranslationBatch batch = llmClient.entity(prompt, input, ReviewTranslationBatch.class,
                () -> ReviewTranslationBatch.mock(sources, target));

        List<String> translations = batch == null ? null : batch.translations();
        if (translations == null || translations.size() != sources.size()
                || translations.stream().anyMatch(t -> t == null || t.isBlank())) {
            throw new LlmException(LlmErrorType.INVALID_OUTPUT, "promptKey=" + PROMPT_KEY
                    + ", 요청 " + sources.size() + "개, 응답 " + (translations == null ? 0 : translations.size()) + "개");
        }
        return translations.stream().map(String::strip).toList();
    }
}
