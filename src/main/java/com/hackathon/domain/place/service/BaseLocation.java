package com.hackathon.domain.place.service;

/**
 * 추천·주변 장소 계산의 기준 위치. 요청 처리에만 쓰고 저장하거나 로그에 남기지 않는다.
 *
 * @param baseStation 기준 역 이름. 좌표로 정했으면 가장 가까운 역 (역 데이터가 없으면 null)
 */
public record BaseLocation(double latitude, double longitude, String baseStation, Source source) {

    public enum Source {
        /** 요청의 lat·lng */
        COORDINATES,
        /** 요청의 stationId */
        STATION,
        /** 둘 다 없어서 app.demo.default-station */
        DEFAULT
    }
}
