package com.hackathon.domain.visit.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "도장을 찍은 장소에서 이어 가기 좋은 코스")
public record RecommendedCourseDto(
        @Schema(description = "코스 ID", example = "independence")
        String courseId,

        @Schema(description = "사용자 언어로 된 코스 이름 (없으면 영어, 그다음 한국어)", example = "독립 코스")
        String name,

        @Schema(description = "다녀온 장소 수", example = "1")
        int visitedCount,

        @Schema(description = "코스의 장소 수", example = "3")
        int totalCount,

        @Schema(description = "모두 다녀왔으면 true", example = "false")
        boolean completed,

        @Schema(description = "코스 장소 (추천 순서)")
        List<CourseStopDto> stops,

        @Schema(description = "다음 추천 장소. 완주했으면 null")
        NextStopDto next
) {
}
