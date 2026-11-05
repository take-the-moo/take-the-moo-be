package com.hackathon.domain.visit.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.repository.PlaceRepository;
import com.hackathon.domain.review.entity.Review;
import com.hackathon.domain.review.repository.ReviewRepository;
import com.hackathon.support.TestUsers;
import com.hackathon.domain.user.service.UserService;
import com.hackathon.domain.visit.entity.Visit;
import com.hackathon.domain.visit.repository.VisitRepository;
import com.hackathon.global.common.Language;
import com.hackathon.global.common.Nationality;
import com.hackathon.global.util.SeoulTime;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * H2는 테스트 컨텍스트끼리 공유되어 다른 테스트의 장소·방문이 섞인다.
 * 그래서 전체 개수가 아니라 이 테스트가 넣은 장소의 핀만 골라 확인한다.
 */
@SpringBootTest(properties = "llm.mock=true")
@AutoConfigureMockMvc
class MapPinTest {

    private static final AtomicInteger CONTENT_ID = new AtomicInteger(700_500);

    @Autowired
    MockMvc mvc;

    @Autowired
    PlaceRepository placeRepository;

    @Autowired
    VisitRepository visitRepository;

    @Autowired
    ReviewRepository reviewRepository;

    @Autowired
    UserService userService;

    private Long savePlace(String name, boolean active) {
        Place place = Place.fromTour(String.valueOf(CONTENT_ID.incrementAndGet()));
        place.updateTourInfo("12", name, "서울 서대문구", 37.5723, 126.9598, "https://example.com/" + name + ".jpg",
                "20261101000000");
        place.applyRules(null, active, false);
        return placeRepository.save(place).getId();
    }

    private String user(Nationality nationality) {
        return TestUsers.create(userService, nationality, nationality.getDefaultLanguage());
    }

    private void visit(String userId, Long placeId, LocalDate date) {
        visitRepository.save(Visit.create(userId, placeId, date));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> pinOf(List<Map<String, Object>> pins, Long placeId) {
        return pins.stream()
                .filter(pin -> ((Number) pin.get("placeId")).longValue() == placeId)
                .findFirst()
                .orElse(null);
    }

    @Test
    @SuppressWarnings("unchecked")
    void 장소별로_다녀간_사람_수와_국적_분포와_한걸음_수를_준다() throws Exception {
        Long visited = savePlace("다녀간곳", true);
        Long empty = savePlace("빈곳", true);
        Long hidden = savePlace("숨긴곳", false);

        LocalDate today = SeoulTime.today();
        String vn1 = user(Nationality.VN);
        String vn2 = user(Nationality.VN);
        String jp = user(Nationality.JP);
        visit(vn1, visited, today);
        visit(vn1, visited, today.minusDays(3)); // 같은 사람이 다른 날 또 와도 1명
        visit(vn2, visited, today.minusDays(1));
        visit(jp, visited, today.minusDays(10));
        visit(jp, hidden, today);
        reviewRepository.save(Review.create(visited, vn1, "좋아요", Language.KO));
        reviewRepository.save(Review.create(visited, jp, "いいね", Language.JA));

        String body = mvc.perform(get("/api/map/pins"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultType").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isArray())
                .andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> pins = JsonPath.read(body, "$.data");

        Map<String, Object> pin = pinOf(pins, visited);
        assertThat(pin).isNotNull();
        assertThat(pin).containsOnlyKeys("placeId", "name", "latitude", "longitude", "imageUrl",
                "visitorCount", "reviewCount", "nationalities");
        assertThat(pin.get("name")).isEqualTo("다녀간곳");
        assertThat(((Number) pin.get("latitude")).doubleValue()).isEqualTo(37.5723);
        assertThat(pin.get("imageUrl")).isEqualTo("https://example.com/다녀간곳.jpg");
        assertThat(((Number) pin.get("visitorCount")).longValue()).isEqualTo(3);
        assertThat(((Number) pin.get("reviewCount")).longValue()).isEqualTo(2);
        assertThat((List<Map<String, Object>>) pin.get("nationalities"))
                .as("많은 순: VN 2명, JP 1명")
                .extracting(n -> n.get("nationality") + ":" + n.get("count"))
                .containsExactly("VN:2", "JP:1");

        Map<String, Object> emptyPin = pinOf(pins, empty);
        assertThat(emptyPin).as("방문이 없어도 노출 장소면 핀이 있다").isNotNull();
        assertThat(((Number) emptyPin.get("visitorCount")).longValue()).isZero();
        assertThat(((Number) emptyPin.get("reviewCount")).longValue()).isZero();
        assertThat((List<?>) emptyPin.get("nationalities")).isEmpty();

        assertThat(pinOf(pins, hidden)).as("노출하지 않는 장소는 빠진다").isNull();

        assertThat(pins.indexOf(pin)).as("방문자 많은 순").isLessThan(pins.indexOf(emptyPin));
    }

    @Test
    @SuppressWarnings("unchecked")
    void 방문자가_같으면_한걸음_많은_순이고_국적_인원이_같으면_국적_코드_순서() throws Exception {
        Long fewerReviews = savePlace("한걸음적은곳", true);
        Long moreReviews = savePlace("한걸음많은곳", true);

        LocalDate today = SeoulTime.today();
        String jp = user(Nationality.JP);
        String vn = user(Nationality.VN);
        // 두 장소 모두 방문자 2명 (JP 1, VN 1). 넣는 순서를 일부러 JP 먼저
        visit(jp, fewerReviews, today);
        visit(vn, fewerReviews, today);
        visit(jp, moreReviews, today);
        visit(vn, moreReviews, today);
        reviewRepository.save(Review.create(moreReviews, vn, "좋아요", Language.KO));

        String body = mvc.perform(get("/api/map/pins"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> pins = JsonPath.read(body, "$.data");

        Map<String, Object> more = pinOf(pins, moreReviews);
        Map<String, Object> fewer = pinOf(pins, fewerReviews);
        assertThat(pins.indexOf(more)).as("방문자가 같으면 한걸음 많은 곳이 먼저").isLessThan(pins.indexOf(fewer));
        assertThat((List<Map<String, Object>>) fewer.get("nationalities"))
                .as("인원이 같으면 국적 코드 순서 (VN이 JP보다 앞)")
                .extracting(n -> n.get("nationality") + ":" + n.get("count"))
                .containsExactly("VN:1", "JP:1");
    }
}
