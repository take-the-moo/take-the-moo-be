package com.hackathon.domain.llm.prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hackathon.domain.llm.exception.LlmErrorType;
import com.hackathon.domain.llm.exception.LlmException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PromptRegistryTest {

    private final PromptRegistry registry = new PromptRegistry(new PromptProperties(Map.of(
            "greet", new PromptProperties.Spec("claude-haiku-5-5", 0.5, 100,
                    "너는 {target}을 돕는다. 말투: {tone}. 없는 변수: {unknown}", Map.of("tone", "친절")))));

    @Test
    void params가_defaults보다_우선하고_없는_변수는_그대로_둔다() {
        LlmPrompt prompt = registry.resolve("greet", Map.of("target", "학생"));

        assertThat(prompt.system()).isEqualTo("너는 학생을 돕는다. 말투: 친절. 없는 변수: {unknown}");
        assertThat(prompt.model()).isEqualTo("claude-haiku-5-5");
        assertThat(prompt.maxTokens()).isEqualTo(100);
    }

    @Test
    void 치환값의_특수문자를_그대로_넣는다() {
        LlmPrompt prompt = registry.resolve("greet", Map.of("target", "$1 \\ {x}"));

        assertThat(prompt.system()).startsWith("너는 $1 \\ {x}을 돕는다.");
    }

    @Test
    void 없는_promptKey면_PROMPT_NOT_FOUND() {
        assertThatThrownBy(() -> registry.resolve("nope", Map.of()))
                .isInstanceOf(LlmException.class)
                .extracting(e -> ((LlmException) e).getErrorCode())
                .isEqualTo(LlmErrorType.PROMPT_NOT_FOUND);
    }
}
