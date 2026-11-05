package com.hackathon.domain.review.dto.response;

/** 장소별 한걸음 수 (F6 흔적 지도) */
public record PlaceReviewCount(Long placeId, long reviewCount) {
}
