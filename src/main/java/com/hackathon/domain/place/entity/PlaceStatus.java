package com.hackathon.domain.place.entity;

/**
 * 추천·후보에는 ACTIVE만 쓴다. 동기화 때마다 다시 계산된다 (규칙은 Place.applyRules).
 * - ACTIVE: 노출. 자동 노출 타입(관광지·문화시설)이거나 보강 파일에 있는 장소
 * - PENDING_REVIEW: 받았지만 노출하지 않는다 (쇼핑 매장 등 자동 노출 타입이 아니고 보강도 없음)
 * - EXCLUDED: 보강 파일 excluded 목록에 있어 숨긴다 (공연장·영화관 등)
 * - INACTIVE: 마지막 동기화에서 TourAPI 목록에 없었다 (방문 기록·한걸음이 참조하므로 지우지 않는다)
 */
public enum PlaceStatus {
    ACTIVE,
    PENDING_REVIEW,
    EXCLUDED,
    INACTIVE
}
