package com.hackathon.domain.llm.prompt;

/**
 * prompts.yml의 한 항목에 요청 params를 치환해 완성한 프롬프트. LlmClient에 그대로 넘긴다.
 */
public record LlmPrompt(
        String key,
        String model,
        Double temperature,
        Integer maxTokens,
        String system
) {
}
