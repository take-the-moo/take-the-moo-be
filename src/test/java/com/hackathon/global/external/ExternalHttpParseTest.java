package com.hackathon.global.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hackathon.domain.place.client.StationItem;
import com.hackathon.domain.place.client.TourPlaceItem;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** 외부 API 응답 형식 처리 (실제 호출 없이 샘플 응답으로) */
class ExternalHttpParseTest {

    private final ExternalHttp http = new ExternalHttp(JsonMapper.builder().build());

    @Test
    void T_Data_역_배열을_읽는다() {
        // 2026-11 실제 응답 형식
        JsonNode root = http.parse("t", """
                [{"outStnNum":"0316","stnKrNm":"독립문","lineNm":"3호선","convX":"126.957748","convY":"37.574571"},
                 {"outStnNum":"1252","stnKrNm":"신촌","lineNm":"경의중앙선","convX":"126.942597","convY":"37.559733"}]
                """);

        var stations = http.toList(root, StationItem.class);

        assertThat(stations).hasSize(2);
        assertThat(stations.getFirst().stnKrNm()).isEqualTo("독립문");
        assertThat(stations.getFirst().isValid()).isTrue();
    }

    @Test
    void 공공데이터_item이_배열_객체_빈값이어도_목록이_된다() {
        JsonNode array = http.parse("t", """
                {"response":{"body":{"items":{"item":[{"contentid":"1","title":"독립문","mapx":"126.95","mapy":"37.57"}]}}}}
                """);
        JsonNode single = http.parse("t", """
                {"response":{"body":{"items":{"item":{"contentid":"1","title":"독립문","mapx":"126.95","mapy":"37.57"}}}}}
                """);
        JsonNode empty = http.parse("t", """
                {"response":{"body":{"items":""}}}
                """);

        assertThat(http.toList(items(array), TourPlaceItem.class)).hasSize(1);
        assertThat(http.toList(items(single), TourPlaceItem.class)).extracting(TourPlaceItem::title).containsExactly("독립문");
        assertThat(http.toList(items(empty), TourPlaceItem.class)).isEmpty();
    }

    @Test
    void JSON이_아닌_오류_응답은_INVALID_RESPONSE() {
        // 공공데이터포털은 키 오류 때 _type=json이어도 XML을 준다
        assertThatThrownBy(() -> http.parse("t", "<OpenAPI_ServiceResponse><cmmMsgHeader>SERVICE_KEY_IS_NOT_REGISTERED_ERROR</cmmMsgHeader></OpenAPI_ServiceResponse>"))
                .isInstanceOf(ExternalApiException.class)
                .extracting(e -> ((ExternalApiException) e).getErrorCode())
                .isEqualTo(ExternalApiErrorType.INVALID_RESPONSE);
    }

    @Test
    void 좌표가_없거나_0이면_장소에서_뺀다() {
        assertThat(new TourPlaceItem("1", "12", "a", null, null, "", "37.5", null, null).hasCoordinates()).isFalse();
        assertThat(new TourPlaceItem("1", "12", "a", null, null, "0", "0", null, null).hasCoordinates()).isFalse();
        assertThat(new TourPlaceItem("1", "12", "a", null, null, "126.9", "37.5", null, null).hasCoordinates()).isTrue();
    }

    private static JsonNode items(JsonNode root) {
        return root.path("response").path("body").path("items").path("item");
    }
}
