package com.hackathon.domain.journey.dto.response;

import com.hackathon.global.common.Language;
import io.swagger.v3.oas.annotations.media.Schema;

/** journey 프롬프트의 구조화 출력이자 응답의 이야기. 필드 설명이 LLM에 전달되는 JSON 스키마가 된다 */
@Schema(description = "AI가 쓴 나의 발자취 이야기")
public record JourneyStoryText(
        @Schema(description = "요청 언어로 쓴 4~6문장 회고 글")
        String storyNative,

        @Schema(description = "storyNative와 같은 내용을 쉬운 한국어(해요체, 짧은 문장) 4~6문장으로")
        String storyEasyKorean
) {

    public static JourneyStoryText mock(int stampCount, int reviewCount, Language language) {
        return new JourneyStoryText(
                "[MOCK] 서대문에서 도장 " + stampCount + "개와 한걸음 " + reviewCount + "개를 남긴 "
                        + language.getKoreanName() + " 이야기입니다.",
                "[MOCK] 서대문에서 " + stampCount + "곳을 걸었어요. 한걸음을 " + reviewCount + "개 남겼어요."
        );
    }
}
