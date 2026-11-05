package com.hackathon.domain.place.dto.response;

import com.hackathon.global.common.Language;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "현장 해설")
public record ExplanationResponseDto(
        @Schema(description = "장소 ID", example = "34")
        Long placeId,

        @Schema(description = "장소 이름 (한국어)", example = "서대문형무소역사관")
        String name,

        @Schema(description = "해설 언어", example = "VI")
        Language language,

        @Schema(description = "요청 언어로 옮긴 해설. KO면 original과 같다",
                example = "Nhà tù Seodaemun do Nhật Bản xây dựng vào năm 1908…")
        String text,

        @Schema(description = "한국어 해설 원문 (팀이 쓴 해설 또는 TourAPI 개요)",
                example = "1908년 일제가 세운 감옥이다…")
        String original,

        @Schema(description = "true면 저장해 둔 번역을 바로 돌려준 것 (AI를 부르지 않음). KO는 번역하지 않으므로 false",
                example = "false")
        boolean cached
) {
}
