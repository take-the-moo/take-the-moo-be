package com.hackathon.domain.recommend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hackathon.domain.place.entity.Station;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "역 선택 목록의 역 (F2)")
public record StationResponseDto(
        @Schema(description = "역 ID. GET /api/now·GET /api/places/nearby의 stationId로 보낸다", example = "12")
        Long stationId,

        @Schema(description = "역 이름", example = "독립문")
        String name,

        @Schema(description = "호선. 같은 이름의 역이 호선마다 따로 온다", example = "3호선")
        String line,

        @Schema(description = "기본 역이면 true. 아무 역도 고르지 않았을 때 이 역을 기준으로 추천한다", example = "true")
        @JsonProperty("isDefault")
        boolean isDefault
) {

    public static StationResponseDto of(Station station, boolean isDefault) {
        return new StationResponseDto(station.getId(), station.getName(), station.getLine(), isDefault);
    }
}
