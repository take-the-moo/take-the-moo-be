package com.hackathon.domain.weather.dto;

public enum WeatherSource {
    /** 기상청 초단기실황 */
    KMA,
    /** 요청 파라미터로 지정한 시연용 날씨 */
    SIMULATED,
    /** 키가 없거나 조회 실패 */
    DEFAULT
}
