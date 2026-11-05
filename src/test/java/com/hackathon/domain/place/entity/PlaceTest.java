package com.hackathon.domain.place.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.hackathon.domain.place.curation.PlaceCuration;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlaceTest {

    @Test
    void 보강_데이터가_있으면_ACTIVE_없으면_검수_전() {
        Place place = Place.fromTour("100");
        assertThat(place.getStatus()).isEqualTo(PlaceStatus.PENDING_REVIEW);

        place.applyCuration(new PlaceCuration("100", "서대문형무소역사관",
                List.of("MORNING", "AFTERNOON"), true, true, "해설 원문"));

        assertThat(place.getStatus()).isEqualTo(PlaceStatus.ACTIVE);
        assertThat(place.getRecommendedTimeSlots()).isEqualTo("MORNING,AFTERNOON");
        assertThat(place.getIndoor()).isTrue();
        assertThat(place.isSensitiveHistory()).isTrue();
        assertThat(place.getExplanation()).isEqualTo("해설 원문");
    }

    @Test
    void 수정일이_바뀌었을_때만_개요를_다시_받는다() {
        Place place = Place.fromTour("100");
        place.updateTourInfo("12", "독립문", "서울", 37.57, 126.95, null, "20260101000000");
        assertThat(place.needsOverview("20260101000000")).isTrue(); // 개요가 아직 없다

        place.updateOverview("개요");
        assertThat(place.needsOverview("20260101000000")).isFalse();
        assertThat(place.needsOverview("20261105000000")).isTrue();
    }
}
