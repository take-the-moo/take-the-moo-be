package com.hackathon.global.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class TimeSlotTest {

    @Test
    void 시각을_시간대로_바꾼다() {
        assertThat(TimeSlot.from(LocalTime.of(6, 0))).contains(TimeSlot.MORNING);
        assertThat(TimeSlot.from(LocalTime.of(10, 59))).contains(TimeSlot.MORNING);
        assertThat(TimeSlot.from(LocalTime.of(11, 0))).contains(TimeSlot.AFTERNOON);
        assertThat(TimeSlot.from(LocalTime.of(16, 59))).contains(TimeSlot.AFTERNOON);
        assertThat(TimeSlot.from(LocalTime.of(17, 0))).contains(TimeSlot.EVENING);
        assertThat(TimeSlot.from(LocalTime.of(20, 59))).contains(TimeSlot.EVENING);
    }

    @Test
    void 밤에는_시간대가_없다() {
        assertThat(TimeSlot.from(LocalTime.of(21, 0))).isEmpty();
        assertThat(TimeSlot.from(LocalTime.of(5, 59))).isEmpty();
    }
}
