package com.hackathon.domain.weather.dto;

/** 추천에 쓰는 날씨. RAINY·SNOWY면 실내 장소만 추천한다 */
public enum WeatherCondition {
    CLEAR,
    RAINY,
    SNOWY;

    public boolean prefersIndoor() {
        return this != CLEAR;
    }

    /**
     * 기상청 강수형태(PTY) → 날씨.
     * 0 없음, 1 비, 2 비/눈, 3 눈, 5 빗방울, 6 빗방울눈날림, 7 눈날림
     */
    public static WeatherCondition fromKmaPty(String pty) {
        return switch (pty == null ? "0" : pty.strip()) {
            case "1", "2", "5", "6" -> RAINY;
            case "3", "7" -> SNOWY;
            default -> CLEAR;
        };
    }
}
