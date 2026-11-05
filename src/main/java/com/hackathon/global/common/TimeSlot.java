package com.hackathon.global.common;

import java.time.LocalTime;
import java.util.Optional;

/**
 * 장소의 추천 방문 시간대. API와 보강 파일에서는 상수 이름(MORNING …)으로 주고받는다.
 * MORNING 06–11 · AFTERNOON 11–17 · EVENING 17–21
 */
public enum TimeSlot {

    MORNING,
    AFTERNOON,
    EVENING;

    /** 시각이 속한 시간대. 밤(21–06)처럼 맞는 시간대가 없으면 비어 있다 */
    public static Optional<TimeSlot> from(LocalTime time) {
        int hour = time.getHour();
        if (hour >= 6 && hour < 11) return Optional.of(MORNING);
        if (hour >= 11 && hour < 17) return Optional.of(AFTERNOON);
        if (hour >= 17 && hour < 21) return Optional.of(EVENING);
        return Optional.empty();
    }
}
