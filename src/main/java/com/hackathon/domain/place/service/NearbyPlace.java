package com.hackathon.domain.place.service;

import com.hackathon.domain.place.entity.Place;

/** 기준 위치에서의 거리(미터)를 붙인 장소. 추천(F2)·주변 장소(F4)가 같이 쓴다 */
public record NearbyPlace(Place place, int distanceMeters) {
}
