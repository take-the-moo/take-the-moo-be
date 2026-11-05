package com.hackathon.domain.journey.sample;

import static org.assertj.core.api.Assertions.assertThat;

import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.repository.PlaceRepository;
import com.hackathon.domain.place.service.PlaceService;
import com.hackathon.domain.review.repository.ReviewRepository;
import com.hackathon.domain.review.service.ReviewService;
import com.hackathon.domain.user.service.UserService;
import com.hackathon.domain.visit.repository.VisitRepository;
import com.hackathon.domain.visit.service.JourneyVisits;
import com.hackathon.domain.visit.service.VisitHistoryService;
import com.hackathon.domain.visit.service.VisitService;
import com.hackathon.global.config.AppProperties;
import com.hackathon.global.util.SeoulTime;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

/** 실제 data/demo-journey.json과 data/courses.json으로 확인한다. 장소를 다른 테스트와 나누지 않으려고 메모리 DB를 따로 쓴다 */
@SpringBootTest(properties = {
        "llm.mock=true",
        "spring.datasource.url=jdbc:h2:mem:demojourney;MODE=MySQL;DATABASE_TO_LOWER=TRUE"
})
class DemoJourneySeederTest {

    private static final String LOGIN_ID = DemoJourneySeeder.LOGIN_ID;
    // 테스트 전용 값
    private static final String TEST_VALUE = "demo-test-value-1234";

    @Autowired
    UserService userService;

    @Autowired
    PlaceService placeService;

    @Autowired
    VisitService visitService;

    @Autowired
    ReviewService reviewService;

    @Autowired
    VisitHistoryService visitHistoryService;

    @Autowired
    PlaceRepository placeRepository;

    @Autowired
    VisitRepository visitRepository;

    @Autowired
    ReviewRepository reviewRepository;

    @Autowired
    JsonMapper jsonMapper;

    private DemoJourneySeeder seeder(String value) throws IOException {
        AppProperties properties = new AppProperties(null, null, null, null,
                new AppProperties.Demo("독립문", null, null, null, value), null, null);
        return new DemoJourneySeeder(userService, placeService, visitService, reviewService, properties, jsonMapper);
    }

    private DemoJourneyFile file() throws IOException {
        try (InputStream in = new ClassPathResource(DemoJourneySeeder.LOCATION).getInputStream()) {
            return jsonMapper.readValue(in, DemoJourneyFile.class);
        }
    }

    private void createPlaces(DemoJourneyFile file) {
        Stream.concat(file.visits().stream().map(DemoJourneyFile.Visit::contentId),
                        file.reviews().stream().map(DemoJourneyFile.Review::contentId))
                .distinct()
                .forEach(contentId -> {
                    Place place = Place.fromTour(contentId);
                    place.updateTourInfo("12", "장소 " + contentId, "서울 서대문구", 37.57, 126.96, null, "20261101000000");
                    place.applyRules(null, true, false);
                    placeRepository.save(place);
                });
    }

    @Test
    void 비밀번호가_없으면_만들지_않고_있으면_한_번만_넣고_다른_비밀번호면_건드리지_않는다() throws Exception {
        assertThat(seeder("").seed()).isTrue();
        assertThat(userService.isLoginIdAvailable(LOGIN_ID)).isTrue();

        assertThat(seeder(TEST_VALUE).seed()).as("장소가 아직 없다").isFalse();
        assertThat(userService.checkCredentials(LOGIN_ID, TEST_VALUE).userId()).isEqualTo(LOGIN_ID);
        assertThat(visitRepository.countByUserId(LOGIN_ID)).isZero();

        DemoJourneyFile file = file();
        createPlaces(file);
        assertThat(seeder(TEST_VALUE).seed()).isTrue();
        assertThat(visitRepository.countByUserId(LOGIN_ID)).isEqualTo(18);
        long reviews = reviewRepository.count();
        assertThat(reviews).isEqualTo(12);

        JourneyVisits visits = visitHistoryService.getJourneyVisits(LOGIN_ID);
        assertThat(visits.stamps()).hasSize(18);
        assertThat(visits.completed()).as("화면의 완주 코스 3개").hasSize(3);
        assertThat(visits.lastVisitDate()).as("지난주 인기에 섞이지 않는다")
                .isBefore(SeoulTime.today().minusDays(14));

        assertThat(seeder(TEST_VALUE).seed()).isTrue();
        assertThat(visitRepository.countByUserId(LOGIN_ID)).isEqualTo(18);
        assertThat(reviewRepository.count()).isEqualTo(reviews);

        assertThat(seeder("other-value-5678").seed()).as("같은 아이디의 다른 계정").isTrue();
        assertThat(visitRepository.countByUserId(LOGIN_ID)).isEqualTo(18);
    }
}
