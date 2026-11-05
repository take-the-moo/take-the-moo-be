package com.hackathon.global.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TimeSlotTest {

    @ParameterizedTest
    @CsvSource({
            "06:00, MORNING",
            "10:59, MORNING",
            "11:00, AFTERNOON",
            "15:00, AFTERNOON",
            "17:00, EVENING",
            "20:59, EVENING"
    })
    void 시각이_속한_시간대(LocalTime time, TimeSlot expected) {
        assertThat(TimeSlot.from(time)).contains(expected);
    }

    @ParameterizedTest
    @CsvSource({"21:00", "23:30", "00:00", "05:59"})
    void 밤에는_맞는_시간대가_없다(LocalTime time) {
        assertThat(TimeSlot.from(time)).isEmpty();
    }
}
