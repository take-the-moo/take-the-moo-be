package com.hackathon.domain.weather.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.hackathon.domain.weather.dto.CurrentWeather;
import com.hackathon.domain.weather.dto.WeatherCondition;
import com.hackathon.domain.weather.dto.WeatherSource;
import java.util.List;
import org.junit.jupiter.api.Test;

class KmaWeatherClientTest {

    @Test
    void 강수형태와_기온으로_날씨를_만든다() {
        CurrentWeather weather = KmaWeatherClient.toWeather(List.of(
                new KmaNowcastItem("PTY", "1"),
                new KmaNowcastItem("T1H", "12.3"),
                new KmaNowcastItem("RN1", "2")));

        assertThat(weather.condition()).isEqualTo(WeatherCondition.RAINY);
        assertThat(weather.temperature()).isEqualTo(12.3);
        assertThat(weather.source()).isEqualTo(WeatherSource.KMA);
    }

    @Test
    void PTY_코드_매핑() {
        assertThat(WeatherCondition.fromKmaPty("0")).isEqualTo(WeatherCondition.CLEAR);
        assertThat(WeatherCondition.fromKmaPty("5")).isEqualTo(WeatherCondition.RAINY);
        assertThat(WeatherCondition.fromKmaPty("3")).isEqualTo(WeatherCondition.SNOWY);
        assertThat(WeatherCondition.fromKmaPty(null)).isEqualTo(WeatherCondition.CLEAR);
    }
}
