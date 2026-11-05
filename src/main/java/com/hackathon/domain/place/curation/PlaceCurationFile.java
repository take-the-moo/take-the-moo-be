package com.hackathon.domain.place.curation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * data/place-curation.json 전체 형식.
 * - excluded: 자동 노출 타입이어도 숨길 contentId (공연장·영화관·어린이공원 등)
 * - excludedNames: excluded와 같은 순서의 이름 (사람이 알아보기 위한 참고용, 코드에서 쓰지 않는다)
 * - places: 덧붙일 정보
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlaceCurationFile(
        List<String> excluded,
        List<String> excludedNames,
        List<PlaceCuration> places
) {
}
