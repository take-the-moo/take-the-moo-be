package com.hackathon.domain.visit.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/** 날짜까지만 내보낸다. 방문 시각과 사용자 위치는 내보내지 않는다 (docs/db.md 공통 규칙) */
@Schema(description = "나의 방문 기록")
public record VisitHistoryResponseDto(
        @Schema(description = "도장 수. 같은 장소를 여러 날 와도 1개다", example = "3")
        int stampCount,

        @Schema(description = "완주한 코스 수", example = "1")
        int completedCourseCount,

        @Schema(description = "도장. 처음 방문한 날이 최근인 순")
        List<Stamp> stamps,

        @Schema(description = "코스 장소 중 아직 안 간 곳. 진행 중인 코스의 남은 장소가 먼저 온다")
        List<PlaceSummary> undiscovered,

        @Schema(description = "코스 전체. 완주 → 많이 진행한 순. 시작 전 코스(visitedCount 0)도 포함한다")
        List<CourseProgressDto> courses,

        @Schema(description = "날짜별 기록. 최신 날짜 순")
        List<Day> days
) {

    @Schema(description = "도장")
    public record Stamp(
            @Schema(description = "장소 ID", example = "1")
            Long placeId,

            @Schema(description = "장소 이름", example = "서대문형무소역사관")
            String name,

            @Schema(description = "처음 방문한 날짜", example = "2026-11-08")
            LocalDate firstVisitedDate
    ) {
    }

    @Schema(description = "장소")
    public record PlaceSummary(
            @Schema(description = "장소 ID", example = "1")
            Long placeId,

            @Schema(description = "장소 이름", example = "서대문자연사박물관")
            String name
    ) {
    }

    @Schema(description = "코스 진행도")
    public record CourseProgressDto(
            @Schema(description = "코스 ID", example = "independence")
            String courseId,

            @Schema(description = "사용자 언어로 된 코스 이름 (없으면 영어, 그다음 한국어)", example = "독립 코스")
            String name,

            @Schema(description = "다녀온 장소 수", example = "3")
            int visitedCount,

            @Schema(description = "코스의 장소 수", example = "3")
            int totalCount,

            @Schema(description = "모두 다녀왔으면 true", example = "true")
            boolean completed,

            @Schema(description = "코스 장소 (추천 순서)")
            List<CourseStopDto> stops
    ) {
    }

    @Schema(description = "하루의 기록")
    public record Day(
            @Schema(description = "날짜", example = "2026-11-08")
            LocalDate date,

            @Schema(description = "그날 다녀온 장소 (도장을 찍은 순서)")
            List<PlaceSummary> places
    ) {
    }
}
