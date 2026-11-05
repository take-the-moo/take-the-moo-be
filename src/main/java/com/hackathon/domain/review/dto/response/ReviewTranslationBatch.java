package com.hackathon.domain.review.dto.response;

import com.hackathon.global.common.Language;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * translate-review 프롬프트의 구조화 출력. 필드 설명이 LLM에 전달되는 JSON 스키마가 된다.
 */
@Schema(description = "한걸음 번역 결과")
public record ReviewTranslationBatch(
        @Schema(description = "입력 texts와 같은 순서·같은 개수의 번역문")
        List<String> translations
) {

    public static ReviewTranslationBatch mock(List<String> texts, Language target) {
        return new ReviewTranslationBatch(texts.stream().map(text -> "[MOCK " + target.name() + "] " + text).toList());
    }
}
