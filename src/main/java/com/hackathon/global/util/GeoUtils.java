package com.hackathon.global.util;

/** 위도·경도 사이 거리(미터). 동네 단위라 하버사인으로 충분하다 */
public final class GeoUtils {

    private static final double EARTH_RADIUS_M = 6_371_000;
    /** 시속 약 4km */
    private static final double WALKING_METERS_PER_MINUTE = 67;

    private GeoUtils() {}

    /** 직선거리로 계산한 도보 시간(분). 실제 경로보다 짧을 수 있는 추정치다 */
    public static int walkingMinutes(int distanceMeters) {
        return Math.max(1, (int) Math.ceil(distanceMeters / WALKING_METERS_PER_MINUTE));
    }

    public static int distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return (int) Math.round(EARTH_RADIUS_M * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
    }
}
