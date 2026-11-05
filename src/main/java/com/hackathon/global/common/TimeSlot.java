package com.hackathon.global.common;

import java.time.LocalTime;
import java.util.Optional;

/**
 * 장소의 추천 방문 시간대. API와 보강 파일에서는 상수 이름(MORNING …)으로 주고받는다.
 * MORNING 06–11 · AFTERNOON 11–17 · EVENING 17–21
 */
public enum TimeSlot {

    MORNING(LocalTime.of(6, 0), LocalTime.of(11, 0)),
    AFTERNOON(LocalTime.of(11, 0), LocalTime.of(17, 0)),
    EVENING(LocalTime.of(17, 0), LocalTime.of(21, 0));

    /** 포함 */
    private final LocalTime start;
    /** 미포함 */
    private final LocalTime end;

    TimeSlot(LocalTime start, LocalTime end) {
        this.start = start;
        this.end = end;
    }

    /**
     * 시각이 속한 시간대. 밤(21–06)처럼 맞는 시간대가 없으면 empty — F2 추천은 이때 시간대 조건을 뺀다.
     * 경계는 시작 포함·끝 미포함이다 (11:00은 AFTERNOON, 21:00은 empty).
     */
    public static Optional<TimeSlot> from(LocalTime time) {
        for (TimeSlot slot : values()) {
            if (!time.isBefore(slot.start) && time.isBefore(slot.end)) return Optional.of(slot);
        }
        return Optional.empty();
    }
}
