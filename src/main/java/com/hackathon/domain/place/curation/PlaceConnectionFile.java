package com.hackathon.domain.place.curation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hackathon.global.common.Nationality;
import java.util.List;
import java.util.Map;

/**
 * data/place-connections.json 전체 형식. places[]는 TourAPI contentId별로 국적 → 연결을 둔다.
 * name은 사람이 알아보기 위한 참고용이다 (코드에서 쓰지 않는다).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlaceConnectionFile(List<Entry> places) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entry(String contentId, String name, Map<Nationality, PlaceConnection> connections) {
    }
}
