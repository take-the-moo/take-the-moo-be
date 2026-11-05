package com.hackathon.domain.visit.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/** 날짜까지만 내보낸다. 방문 시각과 사용자 위치는 내보내지 않는다 (docs/db.md 공통 규칙) */
@Schema(description = "방문 도장")
public record VisitResponseDto(
        @Schema(description = "도장 ID", example = "1")
        Long visitId,

        @Schema(description = "장소 ID", example = "1")
        Long placeId,

        @Schema(description = "장소 이름", example = "서대문형무소역사관")
        String placeName,

        @Schema(description = "방문 날짜", example = "2026-11-08")
        LocalDate visitedDate,

        @Schema(description = "오늘 이미 찍은 도장을 돌려준 것이면 true", example = "false")
        boolean alreadyStamped,

        @Schema(description = "이 장소에 처음 왔으면 true. 다른 날 다시 왔으면 false", example = "true")
        boolean firstVisit,

        @Schema(description = "이어 가기 좋은 코스. 코스에 없는 장소이거나 이어 갈 코스가 없으면 null")
        RecommendedCourseDto recommendedCourse
) {
}
