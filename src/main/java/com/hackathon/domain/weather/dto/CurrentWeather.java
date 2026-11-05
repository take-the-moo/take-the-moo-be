package com.hackathon.domain.weather.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "현재 날씨")
public record CurrentWeather(
        @Schema(description = "날씨", example = "RAINY")
        WeatherCondition condition,

        @Schema(description = "기온(℃). 모르면 null", example = "12.3")
        Double temperature,

        @Schema(description = "출처: KMA(기상청) / SIMULATED(시연용 지정) / DEFAULT(기본값)", example = "KMA")
        WeatherSource source
) {

    public static CurrentWeather defaultWeather() {
        return new CurrentWeather(WeatherCondition.CLEAR, null, WeatherSource.DEFAULT);
    }

    public static CurrentWeather simulated(WeatherCondition condition) {
        return new CurrentWeather(condition, null, WeatherSource.SIMULATED);
    }
}
