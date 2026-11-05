package com.hackathon.global.external;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 외부 API(GET + JSON) 공통 호출기.
 * - 쿼리 값은 URLEncoder로 직접 인코딩한다. 공공데이터포털 Decoding 키의 '+', '/', '='가 그대로 가면 인증이 실패한다
 * - 인증키가 든 URL·원본 예외 메시지는 로그·예외에 남기지 않는다
 * - 공공데이터포털은 키 오류·트래픽 초과 때 _type=json이어도 XML을 주므로 JSON이 아니면 INVALID_RESPONSE로 바꾼다
 */
@Component
public class ExternalHttp {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(20);

    private final RestClient restClient;
    private final JsonMapper jsonMapper;

    public ExternalHttp(JsonMapper jsonMapper) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
        this.jsonMapper = jsonMapper;
    }

    /** apiName은 로그·예외용 이름(예: "TourAPI areaBasedList2"). params 순서를 유지하려면 LinkedHashMap을 넘긴다 */
    public JsonNode getJson(String apiName, String baseUrl, Map<String, String> params) {
        URI uri = URI.create(baseUrl + "?" + query(params));
        String body;
        try {
            body = restClient.get().uri(uri).retrieve().body(String.class);
        } catch (RestClientException e) {
            throw new ExternalApiException(ExternalApiErrorType.CALL_FAILED,
                    apiName + " 호출 실패: " + e.getClass().getSimpleName());
        }
        return parse(apiName, body);
    }

    JsonNode parse(String apiName, String body) {
        String trimmed = body == null ? "" : body.strip();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            throw new ExternalApiException(ExternalApiErrorType.INVALID_RESPONSE,
                    apiName + " JSON이 아닌 응답 (인증키·트래픽 한도 확인): " + preview(trimmed));
        }
        try {
            return jsonMapper.readTree(trimmed);
        } catch (JacksonException e) {
            throw new ExternalApiException(ExternalApiErrorType.INVALID_RESPONSE,
                    apiName + " JSON 파싱 실패: " + e.getClass().getSimpleName());
        }
    }

    /**
     * 배열·단일 객체·빈 값("" 또는 없음)을 모두 목록으로 바꾼다.
     * 공공데이터포털은 결과가 0건이면 items를 ""로, 1건이면 item을 배열 대신 객체로 주는 경우가 있다.
     */
    public <T> List<T> toList(JsonNode node, Class<T> type) {
        if (node == null || node.isMissingNode() || node.isNull()) return List.of();
        try {
            if (node.isArray()) {
                List<T> result = new ArrayList<>(node.size());
                for (int i = 0; i < node.size(); i++) result.add(jsonMapper.treeToValue(node.get(i), type));
                return result;
            }
            if (node.isObject()) return List.of(jsonMapper.treeToValue(node, type));
            return List.of();
        } catch (JacksonException e) {
            throw new ExternalApiException(ExternalApiErrorType.INVALID_RESPONSE,
                    type.getSimpleName() + " 변환 실패: " + e.getClass().getSimpleName());
        }
    }

    private static String query(Map<String, String> params) {
        return params.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    // 오류 본문 앞부분만 남긴다 (공공데이터포털 오류 XML의 returnAuthMsg 등 확인용, 키는 들어 있지 않다)
    private static String preview(String body) {
        String oneLine = body.replaceAll("\\s+", " ");
        return oneLine.length() > 200 ? oneLine.substring(0, 200) + "…" : oneLine;
    }
}
