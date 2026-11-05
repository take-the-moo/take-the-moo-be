package com.hackathon.domain.scene.dto.response;

import com.hackathon.domain.scene.entity.Scene;
import com.hackathon.global.common.Language;
import com.hackathon.global.common.Nationality;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "맞춤 장면")
public record SceneResponseDto(
        @Schema(description = "장소 ID", example = "1")
        Long placeId,

        @Schema(description = "장소 이름", example = "서대문형무소역사관")
        String placeName,

        @Schema(description = "관점 국적", example = "VN")
        Nationality nationality,

        @Schema(description = "이야기 언어", example = "VI")
        Language language,

        @Schema(description = "요청 언어로 쓴 3~4문장 이야기",
                example = "Nơi đây từng là nhà tù do Nhật Bản xây dựng vào năm 1908…")
        String storyNative,

        @Schema(description = "쉬운 한국어 이야기 3~4문장",
                example = "이곳은 1908년에 일제가 세운 감옥이 있던 곳이에요…")
        String storyEasyKorean,

        @Schema(description = "읽는 사람 나라의 역사와 잇는 한 줄",
                example = "Giống như nhà tù Hỏa Lò ở Hà Nội, nơi đây cũng là nơi giam giữ những người mơ về độc lập.")
        String connectionLine,

        @Schema(description = "true면 저장해 둔 이야기를 바로 돌려준 것 (AI를 부르지 않음)", example = "false")
        boolean cached
) {

    public static SceneResponseDto of(Scene scene, String placeName, boolean cached) {
        return new SceneResponseDto(scene.getPlaceId(), placeName, scene.getNationality(), scene.getLanguage(),
                scene.getStoryNative(), scene.getStoryEasyKorean(), scene.getConnectionLine(), cached);
    }
}
