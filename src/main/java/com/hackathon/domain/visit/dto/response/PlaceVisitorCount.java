package com.hackathon.domain.visit.dto.response;

/** 기간 내 장소별 방문자 수 (F2 지난주 인기). 한 사람이 여러 날 와도 1명이다 */
public record PlaceVisitorCount(Long placeId, long visitorCount) {
}
