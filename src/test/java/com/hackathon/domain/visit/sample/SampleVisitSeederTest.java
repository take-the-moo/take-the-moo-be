package com.hackathon.domain.visit.sample;

import static org.assertj.core.api.Assertions.assertThat;

import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.entity.PlaceStatus;
import com.hackathon.domain.place.repository.PlaceRepository;
import com.hackathon.domain.user.service.UserService;
import com.hackathon.domain.visit.entity.Visit;
import com.hackathon.domain.visit.repository.VisitRepository;
import com.hackathon.global.common.Nationality;
import com.hackathon.global.util.SeoulTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 실제 data/sample-visits.json으로 확인한다. 독립문(127425)은 VN 5, CN 3, JP 2, MN 2, UZ 1 = 13명이다.
 * 파일의 독립문 줄을 바꾸면 이 테스트의 기대값도 고친다.
 * H2는 테스트 컨텍스트끼리 공유되어 독립문 장소가 다른 테스트(SampleReviewSeederTest)로 이미 있을 수 있다.
 */
@SpringBootTest(properties = "llm.mock=true")
class SampleVisitSeederTest {

    private static final String DONGNIMMUN = "127425";
    private static final int DONGNIMMUN_VISITORS = 13;
    private static final List<String> FILE_CONTENT_IDS =
            List.of("127425", "130152", "2789102", "128622", "2661502", "2813170", "2773278", "1828571");

    @Autowired
    SampleVisitSeeder seeder;

    @Autowired
    PlaceRepository placeRepository;

    @Autowired
    VisitRepository visitRepository;

    @Autowired
    UserService userService;

    /** 이 테스트가 만든 장소. 끝나면 지운다 */
    private final List<Long> createdPlaces = new ArrayList<>();

    private Long activePlace(String contentId) {
        return placeRepository.findByContentId(contentId)
                .filter(place -> place.getStatus() == PlaceStatus.ACTIVE)
                .orElseGet(() -> {
                    Place place = Place.fromTour(contentId);
                    place.updateTourInfo("12", "독립문", "서울 서대문구", 37.57, 126.96, null, "20261101000000");
                    place.applyRules(null, true, false);
                    Place saved = placeRepository.save(place);
                    createdPlaces.add(saved.getId());
                    return saved;
                })
                .getId();
    }

    /**
     * 공유 H2에 샘플 방문과 독립문 장소를 남기지 않는다.
     * 남기면 다른 테스트가 깨진다 (SampleReviewSeederTest는 독립문이 없다고 보고, NowControllerTest의 지난주 인기 1위가 바뀐다)
     */
    @AfterEach
    void cleanUp() {
        Set<String> sampleUsers = userService.getAllSampleUserIds();
        FILE_CONTENT_IDS.forEach(contentId -> placeRepository.findByContentId(contentId)
                .ifPresent(place -> visitRepository.deleteByPlaceIdAndUserIdIn(place.getId(), sampleUsers)));
        placeRepository.deleteAllById(createdPlaces);
        createdPlaces.clear();
    }

    /** 기간 안에 이 장소를 다녀간 샘플 사용자 수. 다른 테스트(방문 도장)의 실제 사용자 방문은 세지 않는다 */
    private long visitors(Long placeId, LocalDate from, LocalDate to) {
        Set<String> sampleUsers = userService.getAllSampleUserIds();
        return visitRepository.findAll().stream()
                .filter(visit -> visit.getPlaceId().equals(placeId))
                .filter(visit -> sampleUsers.contains(visit.getUserId()))
                .filter(visit -> !visit.getVisitedDate().isBefore(from) && !visit.getVisitedDate().isAfter(to))
                .map(Visit::getUserId)
                .distinct()
                .count();
    }

    @Test
    void 장소마다_국적별_샘플_방문자를_넣고_다시_불러도_수가_같다() {
        Long placeId = activePlace(DONGNIMMUN);
        long missing = FILE_CONTENT_IDS.stream()
                .filter(id -> placeRepository.findByContentId(id).filter(p -> p.getStatus() == PlaceStatus.ACTIVE).isEmpty())
                .count();
        LocalDate today = SeoulTime.today();

        assertThat(seeder.seed()).as("장소가 아직 없는 곳 수").isEqualTo(missing);
        assertThat(visitors(placeId, today.minusDays(30), today)).isEqualTo(DONGNIMMUN_VISITORS);

        assertThat(seeder.seed()).isEqualTo(missing);
        assertThat(visitors(placeId, today.minusDays(30), today)).as("다시 불러도 늘지 않는다").isEqualTo(DONGNIMMUN_VISITORS);

        assertThat(visitors(placeId, today, today)).as("오늘 방문은 없다 (1~10일 전)").isZero();
        assertThat(visitors(placeId, today.minusDays(SampleVisitSeeder.DAYS_SPREAD), today.minusDays(1)))
                .isEqualTo(DONGNIMMUN_VISITORS);
        assertThat(visitors(placeId, today.minusDays(6), today)).as("대부분 최근 7일 안이라 지난주 인기에 잡힌다")
                .isGreaterThan(DONGNIMMUN_VISITORS / 2);
    }

    @Test
    void 국적별_첫_번째_샘플_사용자는_한걸음_작성자이고_방문자다() {
        Long placeId = activePlace(DONGNIMMUN);
        seeder.seed();

        for (Nationality nationality : List.of(Nationality.VN, Nationality.CN, Nationality.JP, Nationality.MN)) {
            String reviewer = userService.getOrCreateSampleUser(nationality);
            assertThat(visitRepository.findVisitedPlaceIds(reviewer)).as(nationality + " 한걸음 작성자").contains(placeId);
        }
    }

    @Test
    void 방문_날짜는_1일_전부터_10일_전_사이() {
        for (Nationality nationality : Nationality.values()) {
            for (int visitor = 0; visitor < 10; visitor++) {
                for (int place = 0; place < 10; place++) {
                    assertThat(SampleVisitSeeder.daysAgo(visitor, place, nationality)).isBetween(1, 10);
                }
            }
        }
    }
}
