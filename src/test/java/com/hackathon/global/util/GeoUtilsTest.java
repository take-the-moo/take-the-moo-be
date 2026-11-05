package com.hackathon.global.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GeoUtilsTest {

    @Test
    void 도보_시간은_올림하고_최소_1분이다() {
        assertThat(GeoUtils.walkingMinutes(409)).isEqualTo(7);
        assertThat(GeoUtils.walkingMinutes(67)).isEqualTo(1);
        assertThat(GeoUtils.walkingMinutes(68)).isEqualTo(2);
        assertThat(GeoUtils.walkingMinutes(0)).isEqualTo(1);
    }
}
