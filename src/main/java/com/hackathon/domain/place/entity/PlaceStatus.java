package com.hackathon.domain.place.entity;

/**
 * 추천·후보에는 ACTIVE만 쓴다.
 * - PENDING_REVIEW: TourAPI에서 새로 들어왔지만 팀 보강(시간대·해설 등)이 아직 없다
 * - ACTIVE: 팀 보강 파일(data/place-curation.json)에 있는 장소
 * - INACTIVE: 마지막 동기화에서 TourAPI 목록에 없었다 (방문 기록·한걸음이 참조하므로 지우지 않는다)
 */
public enum PlaceStatus {
    PENDING_REVIEW,
    ACTIVE,
    INACTIVE
}
