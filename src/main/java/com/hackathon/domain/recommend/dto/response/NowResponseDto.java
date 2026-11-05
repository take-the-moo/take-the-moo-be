package com.hackathon.domain.recommend.dto.response;

import com.hackathon.domain.weather.dto.WeatherCondition;
import com.hackathon.domain.weather.dto.WeatherSource;
import com.hackathon.global.common.TimeSlot;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 추천 결과는 저장하지 않는다 (docs/db.md). 위치(lat/lng)는 응답에도 넣지 않는다 */
@Schema(description = "지금 추천 (F2)")
public record NowResponseDto(
        @Schema(description = "추천 장소")
        RecommendedPlace place,

        @Schema(description = "사용자 언어로 쓴 추천 이유 한 문장. 내 나라의 비슷한 사례로 잇는다",
                example = "Nơi này giống nhà tù Hỏa Lò ở Hà Nội.")
        String reason,

        @Schema(description = "같은 내용의 쉬운 한국어", example = "베트남의 호아로 수용소와 비슷한 곳이에요.")
        String reasonKo,

        @Schema(description = "추천에 쓴 상황")
        Context context,

        @Schema(description = "다른 후보 (가까운 순, 최대 3곳)")
        List<Alternative> alternatives,

        @Schema(description = "지난주 인기 장소 (최근 7일 방문자 많은 순, 최대 3곳). 방문 기록이 없으면 빈 배열")
        List<PopularPlace> weeklyPopular
) {

    @Schema(description = "추천 장소")
    public record RecommendedPlace(
            @Schema(description = "장소 ID. POST /api/scenes·POST /api/explanations에 보낸다", example = "34")
            Long placeId,

            @Schema(description = "장소 이름 (한국어)", example = "서대문형무소역사관")
            String name,

            @Schema(description = "대표 이미지 주소. 없으면 null", nullable = true,
                    example = "https://tong.visitkorea.or.kr/cms/resource/example/prison.jpg")
            String imageUrl,

            @Schema(description = "기준 위치에서의 직선거리 (미터)", example = "197")
            int distanceMeters,

            @Schema(description = "걷는 시간 (분, 분당 67m로 서버가 계산)", example = "3")
            int walkMinutes,

            @Schema(description = "실내 장소인지", example = "true")
            boolean indoor
    ) {
    }

    @Schema(description = "추천에 쓴 상황")
    public record Context(
            @Schema(description = "기준 시각 (서울, HH:mm). time 파라미터가 있으면 그 값", example = "15:00")
            String time,

            @Schema(description = "시간대. 밤(21–06시)이면 null — 시간대 조건 없이 골랐다", example = "AFTERNOON", nullable = true)
            TimeSlot timeSlot,

            @Schema(description = "날씨", example = "RAINY")
            WeatherCondition weather,

            @Schema(description = "기온 (℃). 모르거나 시뮬레이션이면 null", example = "11.0", nullable = true)
            Double temperature,

            @Schema(description = "날씨 출처: KMA(기상청) / SIMULATED(weather 파라미터) / DEFAULT(기본값)", example = "SIMULATED")
            WeatherSource weatherSource,

            @Schema(description = "기준 역 이름. 좌표로 정했으면 가장 가까운 역", example = "독립문", nullable = true)
            String baseStation
    ) {
    }

    @Schema(description = "다른 후보")
    public record Alternative(
            @Schema(description = "장소 ID", example = "4")
            Long placeId,

            @Schema(description = "장소 이름", example = "독립문")
            String name,

            @Schema(description = "기준 위치에서의 직선거리 (미터)", example = "287")
            int distanceMeters
    ) {
    }

    @Schema(description = "지난주 인기 장소")
    public record PopularPlace(
            @Schema(description = "장소 ID", example = "4")
            Long placeId,

            @Schema(description = "장소 이름", example = "독립문")
            String name,

            @Schema(description = "최근 7일 방문자 수 (사람 수. 한 사람이 여러 날 와도 1명)", example = "12")
            long visitorCount
    ) {
    }
}
