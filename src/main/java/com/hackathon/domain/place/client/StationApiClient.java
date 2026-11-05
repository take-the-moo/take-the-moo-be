package com.hackathon.domain.place.client;

import com.hackathon.global.config.AppProperties;
import com.hackathon.global.external.ExternalApiErrorType;
import com.hackathon.global.external.ExternalApiException;
import com.hackathon.global.external.ExternalHttp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * 서울교통빅데이터플랫폼(T Data) 지하철역 좌표정보. 응답은 JSON 배열 하나다.
 * 2026-11 기준 784건이 한 페이지(rowCnt=1000)에 온다.
 */
@Component
public class StationApiClient {

    private static final String API_NAME = "T Data 지하철역 좌표정보";
    private static final int PAGE_SIZE = 1000;
    private static final int MAX_PAGES = 5;

    private final ExternalHttp http;
    private final AppProperties.Station station;

    public StationApiClient(ExternalHttp http, AppProperties properties) {
        this.http = http;
        this.station = properties.station();
    }

    public boolean isConfigured() {
        return !station.apiKey().isBlank();
    }

    public List<StationItem> fetchAll() {
        List<StationItem> result = new ArrayList<>();
        for (int page = 0; page < MAX_PAGES; page++) {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("apikey", station.apiKey());
            params.put("startRow", String.valueOf(page * PAGE_SIZE + 1));
            params.put("rowCnt", String.valueOf(PAGE_SIZE));

            JsonNode root = http.getJson(API_NAME, station.baseUrl(), params);
            if (!root.isArray()) {
                throw new ExternalApiException(ExternalApiErrorType.INVALID_RESPONSE, API_NAME + " 배열이 아닌 응답");
            }
            List<StationItem> rows = http.toList(root, StationItem.class);
            result.addAll(rows);
            if (rows.size() < PAGE_SIZE) break;
        }
        return result;
    }
}
