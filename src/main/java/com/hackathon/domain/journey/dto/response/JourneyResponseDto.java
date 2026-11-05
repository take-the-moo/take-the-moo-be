package com.hackathon.domain.journey.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "나의 발자취. 시각과 위치는 내보내지 않는다")
public record JourneyResponseDto(
        @Schema(description = "도장과 한걸음 중 가장 이른 날짜. 기록이 없으면 null", example = "2026-07-12")
        LocalDate startDate,

        @Schema(description = "도장과 한걸음 중 가장 늦은 날짜. 기록이 없으면 null", example = "2026-10-22")
        LocalDate endDate,

        Stats stats,

        @Schema(description = "순간 카드 전부. 날짜 오름차순, 같은 날이면 첫 도장, 한걸음, 코스 완주 순서")
        List<Moment> moments
) {

    @Schema(description = "숫자")
    public record Stats(
            @Schema(description = "도장 수. 나의 기록과 같다", example = "18")
            int stampCount,

            @Schema(description = "완주한 코스 수. 나의 기록과 같다", example = "3")
            int completedCourseCount,

            @Schema(description = "한걸음 수", example = "12")
            int reviewCount
    ) {
    }

    @Schema(description = "순간 카드")
    public record Moment(
            @Schema(description = "카드 종류. 문구는 이 값을 보고 프론트가 언어별로 표시한다")
            MomentType type,

            @Schema(description = "날짜", example = "2026-08-02")
            LocalDate date,

            @Schema(description = "장소 ID", example = "53")
            Long placeId,

            @Schema(description = "장소 이름 (한국어)", example = "독립문영천시장")
            String placeName,

            @Schema(description = "코스 이름. 코스 완주 카드에만 있다")
            String courseName,

            @Schema(description = "한걸음 내용. 한걸음 카드에만 있다")
            String content,

            @Schema(description = "이미지 주소 (서버 기준 상대 경로). 없으면 null", example = "/api/places/53/image")
            String imageUrl
    ) {
    }
}
