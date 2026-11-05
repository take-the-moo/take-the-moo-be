package com.hackathon.domain.place.curation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 팀이 직접 보강하는 장소 정보 한 건 (src/main/resources/data/place-curation.json).
 * TourAPI에 없는 값만 둔다. contentId는 TourAPI 동기화 후 DB(place.content_id)나 TourAPI 응답에서 확인한다.
 *
 * <pre>
 * {
 *   "contentId": "126xxx",                 // 필수, TourAPI contentid
 *   "name": "서대문형무소역사관",             // 참고용 (사람이 알아보기 위한 값, 저장하지 않는다)
 *   "timeSlots": ["MORNING", "AFTERNOON"],  // 추천 방문 시간대
 *   "indoor": true,                          // 비 오면 실내만 추천
 *   "sensitiveHistory": true,                // 식민지 역사가 얽힌 장소
 *   "explanation": "현장 해설 원문 (한국어)"   // AI는 번역만 한다
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
