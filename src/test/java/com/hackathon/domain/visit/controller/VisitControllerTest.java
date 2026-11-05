package com.hackathon.domain.visit.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hackathon.domain.place.curation.PlaceCuration;
import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.repository.PlaceRepository;
import com.hackathon.domain.user.dto.request.UserCreateRequestDto;
import com.hackathon.domain.user.service.UserService;
import com.hackathon.domain.visit.dto.response.PlaceVisitorCount;
import com.hackathon.domain.visit.entity.Visit;
import com.hackathon.domain.visit.repository.VisitRepository;
import com.hackathon.domain.visit.service.VisitService;
import com.hackathon.global.common.Language;
import com.hackathon.global.common.Nationality;
import com.hackathon.global.common.TimeSlot;
import com.hackathon.global.util.SeoulTime;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 실제 data/courses.json과 실제 장소 좌표로 도장·코스 추천을 검증한다.
 * 외부 동기화와 날씨 조회는 끄고, 장소는 contentId로 직접 넣는다.
 */
@SpringBootTest(properties = {
        "llm.mock=true",
        "app.tour.sync-enabled=false",
        "app.station.sync-enabled=false",
        "app.weather.kma-service-key="
})
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class VisitControllerTest {

    private static final List<TimeSlot> DAYTIME = List.of(TimeSlot.MORNING, TimeSlot.AFTERNOON);
    private static final List<TimeSlot> ALL_DAY = List.of(TimeSlot.MORNING, TimeSlot.AFTERNOON, TimeSlot.EVENING);
    private static final List<TimeSlot> LATE = List.of(TimeSlot.AFTERNOON, TimeSlot.EVENING);

    private static final String FAR_LAT = "37.123456";
    private static final String FAR_LNG = "127.654321";

    @Autowired
    MockMvc mvc;

    @Autowired
    PlaceRepository placeRepository;

    @Autowired
    VisitRepository visitRepository;

    @Autowired
    VisitService visitService;

    @Autowired
    UserService userService;

    Place prison;
    Place gate;
    Place market;
    Place government;
    Place park;
    Place seoJaePil;
    Place tower;
    Place outsideCourse;
    String userId;

    @BeforeEach
    void setUp() {
        prison = upsert("130152", "서대문형무소역사관", 37.574303895, 126.9555345762, true, DAYTIME);
        gate = upsert("127425", "독립문", 37.5724, 126.9595, false, ALL_DAY);
        market = upsert("2789102", "독립문영천시장", 37.5703342733, 126.9618475045, false, LATE);
        government = upsert("2813170", "국립대한민국임시정부기념관", 37.5760930847, 126.9544351508, true, DAYTIME);
        park = upsert("128622", "서대문독립공원", 37.5746723659, 126.9573421635, false, ALL_DAY);
        seoJaePil = upsert("1829610", "송재서재필선생상", 37.5746183894, 126.9573284067, false, ALL_DAY);
        tower = upsert("1825057", "3.1독립선언기념탑", 37.573587, 126.958296, false, ALL_DAY);
        outsideCourse = upsert("visit-test-outside", "코스 밖 장소", 37.5660945924, 126.9382483586, false, ALL_DAY);
        userId = newUser(Nationality.VN, Language.VI);
    }

    private Place upsert(String contentId, String name, double lat, double lng, boolean indoor, List<TimeSlot> slots) {
        Place place = placeRepository.findByContentId(contentId).orElseGet(() -> Place.fromTour(contentId));
        place.updateTourInfo("12", name, "서울 서대문구", lat, lng, null, "20261101000000");
        place.applyRules(new PlaceCuration(contentId, name, slots, indoor, null, null), true, false);
        try {
            return placeRepository.save(place);
        } catch (DataIntegrityViolationException e) {
            return placeRepository.findByContentId(contentId).orElseThrow();
        }
    }

    private String newUser(Nationality nationality, Language language) {
        return userService.create(new UserCreateRequestDto(nationality, language)).userId();
    }

    private ResultActions stampAt(String user, Place place, Place at) throws Exception {
        return stamp(user, place.getId(), String.valueOf(at.getLatitude()), String.valueOf(at.getLongitude()), "");
    }

    private ResultActions stampHere(String user, Place place) throws Exception {
        return stampAt(user, place, place);
    }

    private ResultActions stampHere(String user, Place place, String weather, String time) throws Exception {
        return stamp(user, place.getId(), String.valueOf(place.getLatitude()), String.valueOf(place.getLongitude()),
                ", \"simulatedWeather\": \"%s\", \"simulatedTime\": \"%s\"".formatted(weather, time));
    }

    private ResultActions stamp(String user, Long placeId, String lat, String lng, String extra) throws Exception {
        return mvc.perform(post("/api/visits").contentType(MediaType.APPLICATION_JSON).content("""
                {"userId": "%s", "placeId": %d, "lat": %s, "lng": %s%s}
                """.formatted(user, placeId, lat, lng, extra)));
    }

    @Nested
    class 도장 {

        @Test
        void 장소_근처면_도장을_찍는다() throws Exception {
            stampHere(userId, prison)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.resultType").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.placeId").value(prison.getId()))
                    .andExpect(jsonPath("$.data.placeName").value("서대문형무소역사관"))
                    .andExpect(jsonPath("$.data.visitedDate").value(SeoulTime.today().toString()))
                    .andExpect(jsonPath("$.data.alreadyStamped").value(false))
                    .andExpect(jsonPath("$.data.firstVisit").value(true));

            assertThat(visitRepository.countByUserId(userId)).isEqualTo(1);
        }

        @Test
        void 반경_안의_다른_위치에서도_찍힌다() throws Exception {
            // 형무소역사관에서 독립공원까지 164m
            stampAt(userId, prison, park).andExpect(status().isCreated());
        }

        @Test
        void 반경_밖이면_422이고_저장하지_않는다() throws Exception {
            // 형무소역사관에서 독립문까지 409m
            stampAt(userId, prison, gate)
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("VISIT_TOO_FAR"));

            assertThat(visitRepository.countByUserId(userId)).isZero();
        }

        @Test
        void 같은_날_다시_찍으면_멀리_있어도_기존_도장을_돌려준다() throws Exception {
            Long visitId = idOf(stampHere(userId, prison).andExpect(status().isCreated()));

            stamp(userId, prison.getId(), FAR_LAT, FAR_LNG, "")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.visitId").value(visitId))
                    .andExpect(jsonPath("$.data.alreadyStamped").value(true))
                    .andExpect(jsonPath("$.data.firstVisit").value(true));

            assertThat(visitRepository.countByUserId(userId)).isEqualTo(1);
        }

        @Test
        void 다른_날_다시_오면_새_도장이고_첫_방문이_아니다() throws Exception {
            visitRepository.save(Visit.create(userId, prison.getId(), SeoulTime.today().minusDays(3)));

            stampHere(userId, prison)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.alreadyStamped").value(false))
                    .andExpect(jsonPath("$.data.firstVisit").value(false));
        }

        @Test
        void 없는_사용자와_장소는_404() throws Exception {
            stampHere("없는-사용자", prison)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));

            stamp(userId, 99_999_999L, "37.57", "126.95", "")
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("PLACE_NOT_FOUND"));
        }

        @Test
        void 좌표가_없거나_범위_밖이면_422() throws Exception {
            mvc.perform(post("/api/visits").contentType(MediaType.APPLICATION_JSON).content("""
                            {"userId": "%s", "placeId": %d}
                            """.formatted(userId, prison.getId())))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));

            stamp(userId, prison.getId(), "91", "126.95", "")
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
            stamp(userId, prison.getId(), "37.57", "181", "")
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
        }

        @Test
        void 좌표를_응답과_로그에_남기지_않는다(CapturedOutput output) throws Exception {
            String body = stamp(userId, prison.getId(), FAR_LAT, FAR_LNG, "")
                    .andExpect(status().isUnprocessableContent())
                    .andReturn().getResponse().getContentAsString();

            assertThat(body).doesNotContain(FAR_LAT, FAR_LNG);
            assertThat(output).contains("VISIT_TOO_FAR").doesNotContain(FAR_LAT, FAR_LNG);
        }
    }

    @Nested
    class 코스_추천 {

        @Test
        void 독립_코스를_돌면_진행도가_오르고_완주한다() throws Exception {
            stampHere(userId, prison, "CLEAR", "15:20")
                    .andExpect(jsonPath("$.data.recommendedCourse.courseId").value("independence"))
                    .andExpect(jsonPath("$.data.recommendedCourse.name").value("Hành trình Độc lập"))
                    .andExpect(jsonPath("$.data.recommendedCourse.visitedCount").value(1))
                    .andExpect(jsonPath("$.data.recommendedCourse.totalCount").value(3))
                    .andExpect(jsonPath("$.data.recommendedCourse.completed").value(false))
                    .andExpect(jsonPath("$.data.recommendedCourse.stops[0].visited").value(true))
                    .andExpect(jsonPath("$.data.recommendedCourse.stops[1].visited").value(false))
                    .andExpect(jsonPath("$.data.recommendedCourse.next.placeId").value(gate.getId()))
                    .andExpect(jsonPath("$.data.recommendedCourse.next.walkingMinutes").value(7));

            stampHere(userId, gate, "CLEAR", "15:40")
                    .andExpect(jsonPath("$.data.recommendedCourse.courseId").value("independence"))
                    .andExpect(jsonPath("$.data.recommendedCourse.visitedCount").value(2))
                    .andExpect(jsonPath("$.data.recommendedCourse.next.placeId").value(market.getId()));

            stampHere(userId, market, "CLEAR", "16:00")
                    .andExpect(jsonPath("$.data.recommendedCourse.courseId").value("independence"))
                    .andExpect(jsonPath("$.data.recommendedCourse.visitedCount").value(3))
                    .andExpect(jsonPath("$.data.recommendedCourse.completed").value(true))
                    .andExpect(jsonPath("$.data.recommendedCourse.next").doesNotExist());
        }

        @Test
        void 순서를_바꿔도_진행도가_오른다() throws Exception {
            stampHere(userId, market);
            stampHere(userId, prison)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.recommendedCourse.courseId").value("independence"))
                    .andExpect(jsonPath("$.data.recommendedCourse.visitedCount").value(2));
        }

        @Test
        void 진행이_같으면_남은_거리가_짧은_코스를_추천한다() throws Exception {
            // 독립문이 든 코스 3개 모두 1/3. 남은 거리: 독립공원 617m, 서재필 930m, 독립 1019m
            stampHere(userId, gate)
                    .andExpect(jsonPath("$.data.recommendedCourse.courseId").value("independence-park"));
        }

        @Test
        void 많이_진행한_코스가_남은_거리보다_우선한다() throws Exception {
            visitRepository.save(Visit.create(userId, market.getId(), SeoulTime.today().minusDays(1)));
            visitRepository.save(Visit.create(userId, prison.getId(), SeoulTime.today().minusDays(1)));

            // 독립 코스 3/3 완주가 이번 도장으로 이뤄진다
            stampHere(userId, gate)
                    .andExpect(jsonPath("$.data.recommendedCourse.courseId").value("independence"))
                    .andExpect(jsonPath("$.data.recommendedCourse.completed").value(true));
        }

        @Test
        void 이미_완주한_코스는_다시_추천하지_않는다() throws Exception {
            LocalDate yesterday = SeoulTime.today().minusDays(1);
            visitRepository.save(Visit.create(userId, prison.getId(), yesterday));
            visitRepository.save(Visit.create(userId, gate.getId(), yesterday));
            visitRepository.save(Visit.create(userId, market.getId(), yesterday));

            // 독립문은 다시 왔으므로 독립 코스 완주를 또 보여 주지 않고 다른 코스를 추천한다
            stampHere(userId, gate)
                    .andExpect(jsonPath("$.data.firstVisit").value(false))
                    .andExpect(jsonPath("$.data.recommendedCourse.courseId").value("seo-jae-pil"))
                    .andExpect(jsonPath("$.data.recommendedCourse.visitedCount").value(2));
        }

        @Test
        void 형무소역사관의_코스를_완주한_뒤_다시_오면_추천이_없다() throws Exception {
            LocalDate yesterday = SeoulTime.today().minusDays(1);
            visitRepository.save(Visit.create(userId, prison.getId(), yesterday));
            visitRepository.save(Visit.create(userId, gate.getId(), yesterday));
            visitRepository.save(Visit.create(userId, market.getId(), yesterday));

            stampHere(userId, prison)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.recommendedCourse").doesNotExist());
        }

        @Test
        void 코스에_없는_장소는_추천이_없다() throws Exception {
            stampHere(userId, outsideCourse)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.recommendedCourse").doesNotExist());
        }

        @Test
        void 노출_중이_아닌_장소가_있는_코스는_추천에서_뺀다() throws Exception {
            government.deactivate();
            placeRepository.save(government);

            // 독립공원 코스(617m)가 빠지고 서재필 코스(930m)가 추천된다
            stampHere(userId, gate)
                    .andExpect(jsonPath("$.data.recommendedCourse.courseId").value("seo-jae-pil"));
        }

        @Test
        void 비가_오면_다음_장소로_실내를_고른다() throws Exception {
            stampHere(userId, gate, "RAINY", "15:20")
                    .andExpect(jsonPath("$.data.recommendedCourse.courseId").value("independence-park"))
                    .andExpect(jsonPath("$.data.recommendedCourse.next.placeId").value(government.getId()));
        }

        @Test
        void 규칙을_통과한_곳이_없으면_가까운_곳을_고른다() throws Exception {
            // 남은 독립문·영천시장 모두 실외다
            stampHere(userId, prison, "RAINY", "15:20")
                    .andExpect(jsonPath("$.data.recommendedCourse.next.placeId").value(gate.getId()));
        }

        @Test
        void 코스_이름은_사용자_언어_영어_한국어_순서로_고른다() throws Exception {
            stampHere(newUser(Nationality.JP, Language.JA), prison)
                    .andExpect(jsonPath("$.data.recommendedCourse.name").value("独立コース"));
            stampHere(newUser(Nationality.MN, Language.MN), prison)
                    .andExpect(jsonPath("$.data.recommendedCourse.name").value("Independence Course"));
            stampHere(newUser(Nationality.OTHER, Language.KO), prison)
                    .andExpect(jsonPath("$.data.recommendedCourse.name").value("독립 코스"));
        }
    }

    @Nested
    class 인기_장소 {

        // 다른 테스트의 방문과 섞이지 않도록 먼 과거 날짜를 쓴다
        private final LocalDate from = LocalDate.of(2001, 1, 1);
        private final LocalDate to = LocalDate.of(2001, 1, 7);

        @Test
        void 사람_수가_많은_순으로_기간과_개수를_지킨다() {
            String other = newUser(Nationality.CN, Language.ZH);
            visitRepository.save(Visit.create(userId, 990_001L, from));
            visitRepository.save(Visit.create(userId, 990_001L, from.plusDays(1))); // 같은 사람은 1명
            visitRepository.save(Visit.create(userId, 990_002L, to));
            visitRepository.save(Visit.create(other, 990_002L, to));
            visitRepository.save(Visit.create(other, 990_003L, to.plusDays(1))); // 기간 밖

            assertThat(visitService.topPlaces(from, to, 10)).containsExactly(
                    new PlaceVisitorCount(990_002L, 2), new PlaceVisitorCount(990_001L, 1));
            assertThat(visitService.topPlaces(from, to, 1)).containsExactly(new PlaceVisitorCount(990_002L, 2));
        }
    }

    private Long idOf(ResultActions actions) throws Exception {
        String body = actions.andReturn().getResponse().getContentAsString();
        return Long.valueOf(body.replaceAll(".*\"visitId\":(\\d+).*", "$1"));
    }
}
