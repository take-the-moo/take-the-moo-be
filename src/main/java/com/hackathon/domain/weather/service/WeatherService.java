package com.hackathon.domain.weather.service;

import com.hackathon.domain.weather.client.KmaWeatherClient;
import com.hackathon.domain.weather.dto.CurrentWeather;
import com.hackathon.domain.weather.dto.WeatherCondition;
import com.hackathon.global.config.AppProperties;
import com.hackathon.global.external.ExternalApiException;
import com.hackathon.global.util.SeoulTime;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 현재 날씨. 서대문구 격자 하나라 사용자와 무관하게 같은 값이므로 cacheTtl(기본 10분) 동안 재사용한다.
 * 키가 없거나 조회에 실패해도 예외를 던지지 않는다 (마지막 값 → 없으면 CLEAR 기본값).
 */
@Slf4j
@Service
public class WeatherService {

    private final KmaWeatherClient client;
    private final Duration cacheTtl;
    private final AtomicReference<Cached> cache = new AtomicReference<>();

    public WeatherService(KmaWeatherClient client, AppProperties properties) {
        this.client = client;
        this.cacheTtl = properties.weather().cacheTtl();
    }

    /** simulated가 있으면(시연용 요청 파라미터) 그 값을, 없으면 실제 날씨를 준다 */
    public CurrentWeather resolve(WeatherCondition simulated) {
        return simulated != null ? CurrentWeather.simulated(simulated) : current();
    }

    public CurrentWeather current() {
        if (!client.isConfigured()) return CurrentWeather.defaultWeather();

        Cached cached = cache.get();
        if (cached != null && cached.fetchedAt().plus(cacheTtl).isAfter(Instant.now())) return cached.weather();

        try {
            CurrentWeather fetched = client.fetchNowcast(SeoulTime.now());
            cache.set(new Cached(fetched, Instant.now()));
            return fetched;
        } catch (ExternalApiException e) {
            log.warn("날씨 조회 실패, {} 사용: {}", cached == null ? "기본값" : "이전 값", e.getDetail());
            return cached == null ? CurrentWeather.defaultWeather() : cached.weather();
        }
    }

    private record Cached(CurrentWeather weather, Instant fetchedAt) {
    }
}
