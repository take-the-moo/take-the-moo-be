package com.hackathon.domain.place.curation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 팀이 직접 덧붙이는 장소 정보 한 건 (data/place-curation.json의 places). 필요한 값만 적고, 안 적은 값은 기본값을 쓴다.
 * contentId는 TourAPI 동기화 후 DB(place.content_id)나 TourAPI 응답에서 확인한다.
 *
 * <pre>
 * {
 *   "contentId": "130152",                  // 필수, TourAPI contentid
 *   "name": "서대문형무소역사관",              // 참고용 (저장하지 않는다)
 *   "timeSlots": ["MORNING", "AFTERNOON"],   // MORNING 06–11 · AFTERNOON 11–17 · EVENING 17–21
 *   "indoor": true,                           // 비 오면 실내만 추천
 *   "sensitiveHistory": true,                 // 식민지 역사가 얽힌 장소 → "기억과 평화의 공간" 원칙
 *   "explanation": "현장 해설 원문 (한국어)"    // 없으면 TourAPI 개요를 쓴다. 개요를 고치고 싶을 때만 적는다
 * }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlaceCuration(
        String contentId,
        String name,
        List<String> timeSlots,
        Boolean indoor,
        Boolean sensitiveHistory,
        String explanation
) {
}
