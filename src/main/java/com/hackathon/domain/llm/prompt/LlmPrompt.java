package com.hackathon.domain.llm.prompt;

/**
 * prompts.yml의 한 항목에 요청 params를 치환해 완성한 프롬프트. LlmClient에 그대로 넘긴다.
 * effort·thinking은 선택이다 (null이면 보내지 않아 모델 기본값을 쓴다). 값의 뜻은 prompts.yml 머리 주석 참고
 */
public record LlmPrompt(
        String key,
        String model,
        Double temperature,
        Integer maxTokens,
        String system,
        String effort,
        String thinking
) {

    /** effort·thinking 없이 (모델 기본값) */
    public LlmPrompt(String key, String model, Double temperature, Integer maxTokens, String system) {
        this(key, model, temperature, maxTokens, system, null, null);
    }
}
