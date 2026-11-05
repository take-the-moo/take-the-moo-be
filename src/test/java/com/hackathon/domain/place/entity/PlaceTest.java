package com.hackathon.domain.place.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.hackathon.domain.place.curation.PlaceCuration;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlaceTest {

    @Test
    void 자동_노출_타입은_보강_없이도_ACTIVE_기본값_적용() {
        Place museum = tourPlace("14");
        museum.applyRules(null, true, false);

        assertThat(museum.getStatus()).isEqualTo(PlaceStatus.ACTIVE);
        assertThat(museum.getIndoor()).isTrue();
        assertThat(museum.getRecommendedTimeSlots()).isEqualTo("MORNING,AFTERNOON");
        assertThat(museum.isSensitiveHistory()).isFalse();
        assertThat(museum.getExplanation()).isNull();

        Place park = tourPlace("12");
        park.applyRules(null, true, false);

        assertThat(park.getIndoor()).isFalse();
        assertThat(park.getRecommendedTimeSlots()).isEqualTo("MORNING,AFTERNOON,EVENING");
    }

    @Test
    void 보강_값이_있으면_기본값보다_우선() {
        Place prison = tourPlace("14");
        prison.applyRules(new PlaceCuration("100", "서대문형무소역사관",
                List.of("MORNING"), null, true, "해설 원문"), true, false);

        assertThat(prison.getStatus()).isEqualTo(PlaceStatus.ACTIVE);
        assertThat(prison.getRecommendedTimeSlots()).isEqualTo("MORNING");
        assertThat(prison.getIndoor()).isTrue(); // indoor는 안 적어서 문화시설 기본값
        assertThat(prison.isSensitiveHistory()).isTrue();
        assertThat(prison.getExplanation()).isEqualTo("해설 원문");
    }

    @Test
    void 자동_노출_타입이_아니면_보강된_곳만_노출() {
        Place shop = tourPlace("38");
        shop.applyRules(null, false, false);
        assertThat(shop.getStatus()).isEqualTo(PlaceStatus.PENDING_REVIEW);

        Place market = tourPlace("38");
        market.applyRules(new PlaceCuration("100", "영천시장", null, null, null, null), false, false);
        assertThat(market.getStatus()).isEqualTo(PlaceStatus.ACTIVE);
    }

    @Test
    void 제외_목록이_가장_우선() {
        Place hall = tourPlace("14");
        hall.applyRules(null, true, true);
        assertThat(hall.getStatus()).isEqualTo(PlaceStatus.EXCLUDED);
    }

    @Test
    void 수정일이_바뀌었을_때만_개요를_다시_받는다() {
        Place place = tourPlace("12");
        assertThat(place.needsOverview("20260101000000")).isTrue(); // 개요가 아직 없다

        place.updateOverview("개요");
        assertThat(place.needsOverview("20260101000000")).isFalse();
        assertThat(place.needsOverview("20261105000000")).isTrue();
    }

    @Test
    void 현장_해설은_팀_해설이_없으면_개요를_쓴다() {
        Place place = tourPlace("14");
        place.applyRules(null, true, false);
        assertThat(place.fieldExplanation()).isNull();

        place.updateOverview("TourAPI 개요");
        assertThat(place.fieldExplanation()).isEqualTo("TourAPI 개요");

        place.applyRules(new PlaceCuration("100", "장소", null, null, null, "팀이 쓴 해설"), true, false);
        assertThat(place.fieldExplanation()).isEqualTo("팀이 쓴 해설");
    }

    private static Place tourPlace(String contentTypeId) {
        Place place = Place.fromTour("100");
        place.updateTourInfo(contentTypeId, "장소", "서울", 37.57, 126.95, null, "20260101000000");
        return place;
    }
}
