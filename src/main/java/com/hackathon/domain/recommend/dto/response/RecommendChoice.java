package com.hackathon.domain.recommend.dto.response;

import com.hackathon.global.common.Language;
import com.hackathon.global.common.Nationality;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * recommend 프롬프트의 구조화 출력. 필드 설명이 LLM에 전달되는 JSON 스키마가 된다.
 */
@Schema(description = "추천 선택 결과")
public record RecommendChoice(
        @Schema(description = "candidates 중 고른 장소의 placeId")
        Long placeId,

        @Schema(description = "요청 언어로 쓴 추천 이유 한 문장")
        String reason,

        @Schema(description = "같은 내용의 쉬운 한국어 한 문장")
        String reasonKo
) {

    /** 목 모드: 첫 번째(가장 가까운) 후보와 정해진 문구 */
    public static RecommendChoice mock(Long placeId, String placeName, Nationality nationality, Language language) {
        return new RecommendChoice(placeId,
                "[MOCK " + language.name() + "] " + nationality.getKoreanName() + "의 비슷한 곳을 떠올리게 하는 " + placeName + "이에요.",
                "[MOCK] " + nationality.getKoreanName() + "의 비슷한 곳을 떠올리게 하는 " + placeName + "이에요.");
    }
}
