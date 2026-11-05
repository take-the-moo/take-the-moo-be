package com.hackathon.domain.place.client;

import com.hackathon.global.config.AppProperties;
import com.hackathon.global.external.ExternalApiErrorType;
import com.hackathon.global.external.ExternalApiException;
import com.hackathon.global.external.ExternalHttp;
import com.hackathon.global.external.PublicDataHeader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * 한국관광공사 TourAPI 국문 관광정보(KorService2).
 * 지역은 법정동 코드(lDongRegnCd·lDongSignguCd)로 거른다.
 * 2026-11 실제 응답으로 필드명·성공 코드("0000")·서대문구 코드(410)를 확인했다.
 */
@Component
public class TourApiClient {

    private static final String SUCCESS = "0000";
    private static final int PAGE_SIZE = 100;
    // 무한 루프 방지. 서대문구 관광 타입 몇 개면 몇 페이지 안에 끝난다
    private static final int MAX_PAGES = 30;

    private final ExternalHttp http;
    private final AppProperties.Tour tour;

    public TourApiClient(ExternalHttp http, AppProperties properties) {
        this.http = http;
        this.tour = properties.tour();
    }

    public boolean isConfigured() {
        return !tour.serviceKey().isBlank();
    }

    /** 서대문구의 해당 관광 타입 장소를 전부 받는다 */
    public List<TourPlaceItem> fetchAreaPlaces(String contentTypeId) {
        List<TourPlaceItem> result = new ArrayList<>();
        for (int page = 1; page <= MAX_PAGES; page++) {
            Map<String, String> params = commonParams();
            params.put("numOfRows", String.valueOf(PAGE_SIZE));
            params.put("pageNo", String.valueOf(page));
            params.put("contentTypeId", contentTypeId);
            params.put("lDongRegnCd", tour.lDongRegnCd());
            params.put("lDongSignguCd", tour.lDongSignguCd());

            List<TourPlaceItem> items = items(call("areaBasedList2", params), TourPlaceItem.class);
            result.addAll(items);
            if (items.size() < PAGE_SIZE) break;
        }
        return result;
    }

    /** 장소 개요(overview). 없으면 빈 값 */
    public Optional<String> fetchOverview(String contentId) {
        Map<String, String> params = commonParams();
        params.put("contentId", contentId);
        return items(call("detailCommon2", params), TourDetailItem.class).stream()
                .map(TourDetailItem::overview)
                .filter(overview -> overview != null && !overview.isBlank())
                .findFirst();
    }

    private Map<String, String> commonParams() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("serviceKey", tour.serviceKey());
        params.put("MobileOS", "ETC");
        params.put("MobileApp", "SeodaemunFootprints");
        params.put("_type", "json");
        return params;
    }

    private JsonNode call(String operation, Map<String, String> params) {
        String apiName = "TourAPI " + operation;
        JsonNode root = http.getJson(apiName, tour.baseUrl() + "/" + operation, params);
        JsonNode response = root.path("response");
        PublicDataHeader header = http.toList(response.path("header"), PublicDataHeader.class).stream()
                .findFirst().orElse(null);
        if (header == null || !SUCCESS.equals(header.resultCode())) {
            throw new ExternalApiException(ExternalApiErrorType.INVALID_RESPONSE,
                    apiName + " 실패 응답: " + (header == null ? "header 없음" : header.resultCode() + " " + header.resultMsg()));
        }
        return response;
    }

    private <T> List<T> items(JsonNode response, Class<T> type) {
        return http.toList(response.path("body").path("items").path("item"), type);
    }
}
