package com.hackathon.domain.place.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * translate-explanation 프롬프트의 구조화 출력. 필드 설명이 LLM에 전달되는 JSON 스키마가 된다.
 */
@Schema(description = "현장 해설 번역 결과")
public record ExplanationTranslationResult(
        @Schema(description = "source 전체를 요청 언어로 옮긴 번역문")
        String text
) {

    public static ExplanationTranslationResult mock(String source) {
        return new ExplanationTranslationResult("[번역] " + source);
    }
}
