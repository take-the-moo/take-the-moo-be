package com.hackathon.domain.place.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.hackathon.domain.place.curation.PlaceCuration;
import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.entity.PlaceStatus;
import com.hackathon.global.common.TimeSlot;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 시간대(place_time_slot)가 트랜잭션 밖에서도 읽고 고쳐지는지 확인한다.
 * PlaceSyncService가 트랜잭션 없이 돌기 때문에 이 테스트에도 일부러 @Transactional을 걸지 않는다.
 */
@SpringBootTest(properties = {
        "llm.mock=true",
        "app.tour.sync-enabled=false",
        "app.station.sync-enabled=false"
})
class PlaceRepositoryTest {

    private static final String CONTENT_ID = "test-time-slot";

    @Autowired
    private PlaceRepository placeRepository;

    @AfterEach
    void cleanUp() {
        placeRepository.findByContentId(CONTENT_ID).ifPresent(placeRepository::delete);
    }

    @Test
    void 동기화처럼_다시_읽어_시간대를_바꾸면_새_값으로_교체된다() {
        Place place = Place.fromTour(CONTENT_ID);
        place.updateTourInfo("14", "장소", "서울", 37.57, 126.95, null, "20260101000000");
        place.applyRules(curationOf(TimeSlot.MORNING), true, false);
        placeRepository.save(place);

        Place synced = placeRepository.findByContentId(CONTENT_ID).orElseThrow();
        assertThat(synced.getTimeSlots()).containsExactly(TimeSlot.MORNING);

        synced.applyRules(curationOf(TimeSlot.AFTERNOON, TimeSlot.EVENING), true, false);
        placeRepository.save(synced);

        assertThat(placeRepository.findByContentId(CONTENT_ID).orElseThrow().getTimeSlots())
                .containsExactlyInAnyOrder(TimeSlot.AFTERNOON, TimeSlot.EVENING);
    }

    @Test
    void 같은_시간대로_다시_동기화해도_그대로다() {
        Place place = Place.fromTour(CONTENT_ID);
        place.updateTourInfo("14", "장소", "서울", 37.57, 126.95, null, "20260101000000");
        place.applyRules(curationOf(TimeSlot.MORNING, TimeSlot.AFTERNOON), true, false);
        placeRepository.save(place);

        Place synced = placeRepository.findByContentId(CONTENT_ID).orElseThrow();
        synced.applyRules(curationOf(TimeSlot.MORNING, TimeSlot.AFTERNOON), true, false);
        placeRepository.save(synced);

        assertThat(placeRepository.findByContentId(CONTENT_ID).orElseThrow().getTimeSlots())
                .containsExactlyInAnyOrder(TimeSlot.MORNING, TimeSlot.AFTERNOON);
    }

    @Test
    void 목록_조회_결과도_트랜잭션_밖에서_시간대를_읽을_수_있다() {
        Place place = Place.fromTour(CONTENT_ID);
        place.updateTourInfo("14", "장소", "서울", 37.57, 126.95, null, "20260101000000");
        place.applyRules(null, true, false);
        placeRepository.save(place);

        Place found = placeRepository.findAllByStatus(PlaceStatus.ACTIVE).stream()
                .filter(p -> CONTENT_ID.equals(p.getContentId()))
                .findFirst()
                .orElseThrow();

        assertThat(found.getTimeSlots()).containsExactlyInAnyOrder(TimeSlot.MORNING, TimeSlot.AFTERNOON);
    }

    private static PlaceCuration curationOf(TimeSlot... slots) {
        return new PlaceCuration(CONTENT_ID, "장소", List.of(slots), null, null, null);
    }
}
