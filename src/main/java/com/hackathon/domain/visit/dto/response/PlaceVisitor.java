package com.hackathon.domain.visit.dto.response;

/** 장소를 다녀간 사람 한 명 (F6 흔적 지도 집계용). 날짜·시각은 담지 않는다 */
public record PlaceVisitor(Long placeId, String userId) {
}
