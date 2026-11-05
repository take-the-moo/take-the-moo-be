package com.hackathon.domain.visit.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "다음 추천 장소")
public record NextStopDto(
        @Schema(description = "장소 ID", example = "2")
        Long placeId,

        @Schema(description = "장소 이름", example = "독립문")
        String name,

        @Schema(description = "도보 시간(분). 직선거리로 계산한 추정치라 실제보다 짧을 수 있다", example = "7")
        int walkingMinutes
) {
}
