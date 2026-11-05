package com.hackathon.domain.weather.client;

import com.hackathon.global.external.PublicDataHeader;
import com.hackathon.domain.weather.dto.CurrentWeather;
import com.hackathon.domain.weather.dto.WeatherCondition;
import com.hackathon.domain.weather.dto.WeatherSource;
import com.hackathon.global.config.AppProperties;
import com.hackathon.global.external.ExternalApiErrorType;
import com.hackathon.global.external.ExternalApiException;
import com.hackathon.global.external.ExternalHttp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * 기상청 단기예보 조회서비스의 초단기실황(getUltraSrtNcst). 서대문구 격자(nx, ny) 하나만 조회한다.
 * TODO(키 발급 후): 실제 응답으로 성공 코드("00")와 발표 시각 규칙을 확인한다.
 */
@Component
public class KmaWeatherClient {

    private static final String API_NAME = "기상청 초단기실황";
    private static final String SUCCESS = "00";
    // 매시 정각 관측 자료가 40분쯤 올라온다. 그 전에는 한 시간 전 자료를 요청한다
    private static final int PUBLISH_MINUTE = 40;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH00");

    private final ExternalHttp http;
    private final AppProperties.Weather weather;

    public KmaWeatherClient(ExternalHttp http, AppProperties properties) {
        this.http = http;
        this.weather = properties.weather();
    }

    public boolean isConfigured() {
        return !weather.kmaServiceKey().isBlank();
    }

    public CurrentWeather fetchNowcast(LocalDateTime now) {
        LocalDateTime base = now.getMinute() < PUBLISH_MINUTE ? now.minusHours(1) : now;

        Map<String, String> params = new LinkedHashMap<>();
        params.put("serviceKey", weather.kmaServiceKey());
        params.put("pageNo", "1");
        params.put("numOfRows", "10");
        params.put("dataType", "JSON");
        params.put("base_date", base.format(DATE));
        params.put("base_time", base.format(HOUR));
        params.put("nx", String.valueOf(weather.nx()));
        params.put("ny", String.valueOf(weather.ny()));

        JsonNode response = http.getJson(API_NAME, weather.kmaBaseUrl() + "/getUltraSrtNcst", params).path("response");
        PublicDataHeader header = http.toList(response.path("header"), PublicDataHeader.class).stream()
                .findFirst().orElse(null);
        if (header == null || !SUCCESS.equals(header.resultCode())) {
            throw new ExternalApiException(ExternalApiErrorType.INVALID_RESPONSE,
                    API_NAME + " 실패 응답: " + (header == null ? "header 없음" : header.resultCode() + " " + header.resultMsg()));
        }
        return toWeather(http.toList(response.path("body").path("items").path("item"), KmaNowcastItem.class));
    }

    static CurrentWeather toWeather(List<KmaNowcastItem> items) {
        String pty = null;
        Double temperature = null;
        for (KmaNowcastItem item : items) {
            if ("PTY".equals(item.category())) pty = item.obsrValue();
            if ("T1H".equals(item.category())) temperature = parseDouble(item.obsrValue());
        }
        return new CurrentWeather(WeatherCondition.fromKmaPty(pty), temperature, WeatherSource.KMA);
    }

    private static Double parseDouble(String value) {
        try {
            return value == null ? null : Double.valueOf(value.strip());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
