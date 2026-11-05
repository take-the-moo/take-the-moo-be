package com.hackathon.domain.scene.dto.response;

import com.hackathon.global.common.Language;
import com.hackathon.global.common.Nationality;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * scene 프롬프트의 구조화 출력. 필드 설명이 LLM에 전달되는 JSON 스키마가 된다.
 */
@Schema(description = "AI가 만든 장소 이야기")
public record SceneStory(
        @Schema(description = "요청 언어로 쓴 3~4문장 이야기. 이 장소가 무엇이고 왜 의미 있는지")
        String storyNative,

        @Schema(description = "storyNative와 같은 내용을 쉬운 한국어(해요체, 짧은 문장) 3~4문장으로")
        String storyEasyKorean,

        @Schema(description = "읽는 사람 나라의 역사·문화와 이 장소를 잇는 한 문장. 요청 언어로")
        String connectionLine
) {

    public static SceneStory mock(String placeName, Nationality nationality, Language language) {
        return new SceneStory(
                "[MOCK] " + placeName + "에 대한 " + language.getKoreanName() + " 이야기입니다. "
                        + nationality.getKoreanName() + " 유학생의 눈으로 이 장소를 소개합니다.",
                "[MOCK] " + placeName + "은 서대문에 있는 곳이에요. 이곳의 이야기를 쉬운 한국어로 들려줘요.",
                "[MOCK] " + nationality.getKoreanName() + "의 역사와 " + placeName + "을 잇는 한 줄입니다."
        );
    }
}
