package com.hackathon.domain.visit.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hackathon.domain.place.curation.PlaceCuration;
import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.repository.PlaceRepository;
import com.hackathon.domain.user.dto.request.UserCreateRequestDto;
import com.hackathon.domain.user.service.UserService;
import com.hackathon.domain.visit.entity.Visit;
import com.hackathon.domain.visit.repository.VisitRepository;
import com.hackathon.global.common.Language;
import com.hackathon.global.common.Nationality;
import com.hackathon.global.common.TimeSlot;
import com.hackathon.global.util.SeoulTime;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 실제 data/courses.json과 실제 장소 좌표로 나의 기록을 검증한다.
 * 다른 테스트의 장소도 같은 DB에 있으므로 전체 개수가 아니라 여기서 넣은 장소만 기준으로 본다.
 */
@SpringBootTest(properties = {
        "llm.mock=true",
        "app.tour.sync-enabled=false",
        "app.station.sync-enabled=false",
        "app.weather.kma-service-key="
})
@AutoConfigureMockMvc
class VisitHistoryTest {

    private static final List<TimeSlot> DAYTIME = List.of(TimeSlot.MORNING, TimeSlot.AFTERNOON);
    private static final List<TimeSlot> ALL_DAY = List.of(TimeSlot.MORNING, TimeSlot.AFTERNOON, TimeSlot.EVENING);
    private static final List<TimeSlot> LATE = List.of(TimeSlot.AFTERNOON, TimeSlot.EVENING);

    @Autowired
    MockMvc mvc;

    @Autowired
    PlaceRepository placeRepository;

    @Autowired
    VisitRepository visitRepository;

    @Autowired
    UserService userService;

    Place prison;
    Place gate;
    Place market;
    Place government;
    Place park;
    Place seoJaePil;
    String userId;
    LocalDate today;

    @BeforeEach
    void setUp() {
        prison = upsert("130152", "서대문형무소역사관", 37.574303895, 126.9555345762, true, DAYTIME);
        gate = upsert("127425", "독립문", 37.5724, 126.9595, false, ALL_DAY);
        market = upsert("2789102", "독립문영천시장", 37.5703342733, 126.9618475045, false, LATE);
        government = upsert("2813170", "국립대한민국임시정부기념관", 37.5760930847, 126.9544351508, true, DAYTIME);
        park = upsert("128622", "서대문독립공원", 37.5746723659, 126.9573421635, false, ALL_DAY);
        seoJaePil = upsert("1829610", "송재서재필선생상", 37.5746183894, 126.9573284067, false, ALL_DAY);
        userId = newUser(Language.KO);
        today = SeoulTime.today();
    }

    private Place upsert(String contentId, String name, double lat, double lng, boolean indoor, List<TimeSlot> slots) {
        Place place = placeRepository.findByContentId(contentId).orElseGet(() -> Place.fromTour(contentId));
        place.updateTourInfo("12", name, "서울 서대문구", lat, lng, null, "20261101000000");
        place.applyRules(new PlaceCuration(contentId, name, slots, indoor, null, null), true, false);
        return placeRepository.save(place);
    }

    private String newUser(Language language) {
        return userService.create(new UserCreateRequestDto(Nationality.VN, language)).userId();
    }

    private void visit(Place place, LocalDate date) {
        visitRepository.save(Visit.create(userId, place.getId(), date));
    }

    private ResultActions history(String user) throws Exception {
        return mvc.perform(get("/api/users/{userId}/visits", user));
    }

    private static String course(String courseId) {
        return "$.data.courses[?(@.courseId == '" + courseId + "')]";
    }

    @Test
    void 없는_사용자는_404() throws Exception {
        history("없는-사용자")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void 방문이_없으면_기록은_비고_코스와_안_간_장소는_나온다() throws Exception {
        history(userId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stampCount").value(0))
                .andExpect(jsonPath("$.data.completedCourseCount").value(0))
                .andExpect(jsonPath("$.data.stamps", hasSize(0)))
                .andExpect(jsonPath("$.data.days", hasSize(0)))
                .andExpect(jsonPath(course("independence") + ".visitedCount", hasItem(0)))
                .andExpect(jsonPath("$.data.undiscovered[*].name", hasItem("서대문형무소역사관")));
    }

    @Test
    void 날짜별로_묶고_최신_날짜가_먼저다() throws Exception {
        visit(prison, today.minusDays(7));
        visit(gate, today);
        visit(market, today);

        history(userId)
                .andExpect(jsonPath("$.data.days", hasSize(2)))
                .andExpect(jsonPath("$.data.days[0].date").value(today.toString()))
                .andExpect(jsonPath("$.data.days[0].places[0].name").value("독립문"))
                .andExpect(jsonPath("$.data.days[0].places[1].name").value("독립문영천시장"))
                .andExpect(jsonPath("$.data.days[1].date").value(today.minusDays(7).toString()))
                .andExpect(jsonPath("$.data.days[1].places[0].name").value("서대문형무소역사관"));
    }

    @Test
    void 같은_장소를_다시_와도_도장은_하나이고_처음_방문일을_준다() throws Exception {
        visit(prison, today.minusDays(7));
        visit(prison, today);
        visit(gate, today.minusDays(3));

        history(userId)
                .andExpect(jsonPath("$.data.stampCount").value(2))
                .andExpect(jsonPath("$.data.stamps[0].name").value("독립문"))
                .andExpect(jsonPath("$.data.stamps[1].name").value("서대문형무소역사관"))
                .andExpect(jsonPath("$.data.stamps[1].firstVisitedDate").value(today.minusDays(7).toString()))
                .andExpect(jsonPath("$.data.days", hasSize(3)));
    }

    @Test
    void 같은_날_찍은_도장은_나중에_찍은_것이_먼저다() throws Exception {
        visit(prison, today);
        visit(gate, today);

        history(userId)
                .andExpect(jsonPath("$.data.stamps[0].name").value("독립문"))
                .andExpect(jsonPath("$.data.stamps[1].name").value("서대문형무소역사관"));
    }

    @Test
    void 코스는_완주_많이_진행한_순이고_완주_수를_센다() throws Exception {
        visit(prison, today);
        visit(gate, today);
        visit(market, today);

        history(userId)
                .andExpect(jsonPath("$.data.completedCourseCount").value(1))
                .andExpect(jsonPath("$.data.courses[0].courseId").value("independence"))
                .andExpect(jsonPath("$.data.courses[0].completed").value(true))
                .andExpect(jsonPath("$.data.courses[0].visitedCount").value(3))
                .andExpect(jsonPath("$.data.courses[0].stops[0].visited").value(true))
                .andExpect(jsonPath("$.data.courses[1].courseId").value("seo-jae-pil"))
                .andExpect(jsonPath("$.data.courses[1].completed").value(false))
                .andExpect(jsonPath("$.data.courses[1].visitedCount").value(2));
    }

    @Test
    void 안_간_장소는_진행_중인_코스의_것이_먼저이고_다녀온_곳은_빠진다() throws Exception {
        visit(gate, today);
        visit(market, today);

        // 2/3로 가장 앞선 코스는 독립 코스(파일 순서 먼저)이고 남은 곳은 형무소역사관이다
        history(userId)
                .andExpect(jsonPath("$.data.undiscovered[0].name").value("서대문형무소역사관"))
                .andExpect(jsonPath("$.data.undiscovered[*].name", not(hasItem("독립문"))))
                .andExpect(jsonPath("$.data.undiscovered[*].name", not(hasItem("독립문영천시장"))));
    }

    @Test
    void 코스_이름은_사용자_언어로_준다() throws Exception {
        history(newUser(Language.VI))
                .andExpect(jsonPath(course("independence") + ".name", hasItem("Hành trình Độc lập")));
        history(newUser(Language.MN))
                .andExpect(jsonPath(course("independence") + ".name", hasItem("Independence Course")));
    }

    @Test
    void 다녀온_장소가_비활성이_되어도_기록과_완주는_남는다() throws Exception {
        visit(prison, today);
        visit(gate, today);
        visit(market, today);
        gate.deactivate();
        placeRepository.save(gate);

        history(userId)
                .andExpect(jsonPath("$.data.stampCount").value(3))
                .andExpect(jsonPath("$.data.stamps[*].name", hasItem("독립문")))
                .andExpect(jsonPath("$.data.days[0].places[*].name", hasItem("독립문")))
                .andExpect(jsonPath("$.data.completedCourseCount").value(1))
                .andExpect(jsonPath("$.data.courses[0].courseId").value("independence"));
    }

    @Test
    void 안_간_장소가_비활성이면_그_코스를_뺀다() throws Exception {
        visit(gate, today);
        government.deactivate();
        placeRepository.save(government);

        history(userId)
                .andExpect(jsonPath(course("independence-park"), hasSize(0)))
                .andExpect(jsonPath(course("independence"), hasSize(1)))
                .andExpect(jsonPath("$.data.undiscovered[*].name", not(hasItem("국립대한민국임시정부기념관"))));
    }

    @Test
    void 도장_응답도_다녀온_장소가_비활성이면_코스를_유지한다() throws Exception {
        visit(gate, today.minusDays(1));
        gate.deactivate();
        placeRepository.save(gate);

        // 독립문은 비활성이지만 이미 다녀왔으므로 독립 코스를 계속 이어 갈 수 있다
        mvc.perform(post("/api/visits").contentType(MediaType.APPLICATION_JSON).content("""
                        {"userId": "%s", "placeId": %d, "lat": %s, "lng": %s}
                        """.formatted(userId, prison.getId(), prison.getLatitude(), prison.getLongitude())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.recommendedCourse.courseId").value("independence"))
                .andExpect(jsonPath("$.data.recommendedCourse.visitedCount").value(2))
                .andExpect(jsonPath("$.data.recommendedCourse.next.placeId").value(market.getId()));
    }

    @Test
    void 찾을_수_없는_장소의_기록만_빼고_응답한다() throws Exception {
        visit(prison, today);
        visitRepository.save(Visit.create(userId, 99_999_999L, today));

        history(userId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stampCount").value(1))
                .andExpect(jsonPath("$.data.stamps[0].name").value("서대문형무소역사관"))
                .andExpect(jsonPath("$.data.days[0].places", hasSize(1)));
    }

    @Test
    void 응답에_시각과_좌표가_없다() throws Exception {
        visit(prison, today);

        String body = history(userId).andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("createdAt", "updatedAt", "latitude", "longitude", "\"lat\"", "\"lng\"",
                userId);
    }
}
