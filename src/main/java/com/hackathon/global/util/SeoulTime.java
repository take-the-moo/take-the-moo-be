package com.hackathon.global.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** 서버 시간대와 무관하게 서울 기준으로 "오늘"·"지금"을 계산한다 */
public final class SeoulTime {

    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private SeoulTime() {}

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }
}
