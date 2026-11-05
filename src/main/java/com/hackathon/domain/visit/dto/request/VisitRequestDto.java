package com.hackathon.domain.visit.dto.request;

import com.hackathon.domain.weather.dto.WeatherCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

/** 좌표는 거리 계산에만 쓰고 저장·로그하지 않는다. URL에 남지 않도록 본문으로 받는다 */
@Schema(description = "방문 도장 요청")
public record VisitRequestDto(
        @Schema(description = "POST /api/users가 돌려준 userId", example = "3f2b8c1e-7a4d-4e9b-9c2a-1d5e6f7a8b9c",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "사용자 ID가 없습니다. 처음 설정을 다시 해 주세요.")
        @Size(max = 36, message = "올바른 사용자 ID가 아닙니다.")
        String userId,

        @Schema(description = "도장을 찍을 장소 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "장소를 선택해 주세요.")
        @Positive(message = "올바른 장소 ID가 아닙니다.")
        Long placeId,

        @Schema(description = "현재 위도. 저장하지 않는다", example = "37.5743", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "위치를 확인할 수 없습니다.")
        @DecimalMin(value = "-90", message = "올바른 위치가 아닙니다.")
        @DecimalMax(value = "90", message = "올바른 위치가 아닙니다.")
        Double lat,

        @Schema(description = "현재 경도. 저장하지 않는다", example = "126.9555", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "위치를 확인할 수 없습니다.")
        @DecimalMin(value = "-180", message = "올바른 위치가 아닙니다.")
        @DecimalMax(value = "180", message = "올바른 위치가 아닙니다.")
        Double lng,

        @Schema(description = "시연용 날씨 (선택). 비우면 실제 날씨", example = "RAINY")
        WeatherCondition simulatedWeather,

        @Schema(description = "시연용 시각 (선택). 비우면 현재 시각", type = "string", example = "15:20")
        LocalTime simulatedTime
) {
}
