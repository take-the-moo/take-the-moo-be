package com.hackathon.domain.place.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.entity.PlaceStatus;
import com.hackathon.domain.place.repository.PlaceRepository;
import com.hackathon.global.common.Nationality;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "llm.mock=true")
class PlaceServiceTest {

    @Autowired
    PlaceService placeService;

    @Autowired
    PlaceRepository placeRepository;

    private Place savePlace(String contentId, boolean active) {
        Place place = Place.fromTour(contentId);
        place.updateTourInfo("12", "장소 " + contentId, "서울 서대문구", 37.57, 126.96, null, "20261101000000");
        place.applyRules(null, active, false);
        return placeRepository.save(place);
    }

    @Test
    void 노출_중인_장소만_돌려준다() {
        Long active = savePlace("400001", true).getId();
        Long pending = savePlace("400002", false).getId();

        assertThat(placeService.getActivePlaces())
                .allMatch(place -> place.getStatus() == PlaceStatus.ACTIVE)
                .extracting(Place::getId)
                .contains(active)
                .doesNotContain(pending);
    }

    @Test
    void 트랜잭션_밖에서도_시간대를_읽을_수_있다() {
        Long active = savePlace("400003", true).getId();

        Place place = placeService.getActivePlaces().stream()
                .filter(p -> p.getId().equals(active)).findFirst().orElseThrow();

        assertThat(place.getTimeSlots()).isNotEmpty();
    }

    @Test
    void 장소와_국적으로_내_나라_연결을_찾는다() {
        Place prison = Place.fromTour("130152"); // 서대문형무소역사관 (data/place-connections.json)

        assertThat(placeService.findConnection(prison, Nationality.VN)).isPresent();
        assertThat(placeService.findConnection(prison, Nationality.OTHER)).isEmpty();
        assertThat(placeService.findConnection(Place.fromTour("400009"), Nationality.VN)).isEmpty();
    }
}
