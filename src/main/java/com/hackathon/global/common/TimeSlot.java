package com.hackathon.global.common;

/**
 * 장소의 추천 방문 시간대. API와 보강 파일에서는 상수 이름(MORNING …)으로 주고받는다.
 * MORNING 06–11 · AFTERNOON 11–17 · EVENING 17–21
 */
public enum TimeSlot {

    MORNING,
    AFTERNOON,
    EVENING
}
