package com.hackathon.domain.visit.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "코스의 장소")
public record CourseStopDto(
        @Schema(description = "장소 ID", example = "1")
        Long placeId,

        @Schema(description = "장소 이름", example = "서대문형무소역사관")
        String name,

        @Schema(description = "날짜와 상관없이 다녀왔으면 true", example = "true")
        boolean visited
) {
}
