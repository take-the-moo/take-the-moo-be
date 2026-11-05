package com.hackathon.domain.recommend.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hackathon.domain.llm.exception.LlmErrorType;
import com.hackathon.domain.llm.exception.LlmException;
import com.hackathon.domain.llm.service.LlmClient;
import com.hackathon.domain.place.curation.PlaceConnection;
import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.entity.Station;
import com.hackathon.domain.place.repository.PlaceRepository;
import com.hackathon.domain.place.repository.StationRepository;
import com.hackathon.domain.place.service.PlaceService;
import com.hackathon.domain.recommend.dto.response.RecommendChoice;
import com.hackathon.support.TestUsers;
import com.hackathon.domain.user.service.UserService;
import com.hackathon.global.common.Language;
import com.hackathon.domain.visit.entity.Visit;
import com.hackathon.domain.visit.repository.VisitRepository;
import com.hackathon.global.common.Nationality;
import com.hackathon.global.util.SeoulTime;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 목 모드로 추천 규칙을 검증한다. 목 응답은 가장 가까운 후보를 고른다.
 * H2는 테스트 컨텍스트끼리 공유되고 다른 테스트의 장소는 서울·울릉도·제주에 있다.
 * 이 테스트는 테스트마다 부산 근처의 다른 곳에 역과 장소를 둔다 (후보 6곳 중 가까운 쪽이 늘 이 테스트의 장소다).
 */
@SpringBootTest(properties = "llm.mock=true")
@AutoConfigureMockMvc
class NowControllerTest {

    private static final AtomicInteger CONTENT_ID = new AtomicInteger(100_000);
    private static final AtomicInteger AREA = new AtomicInteger();
    private static final double METERS_PER_DEGREE = 6_371_000 * Math.PI / 180;
    private static final String OUTDOOR = "12";
    /** 문화시설: 실내, 시간대 MORNING·AFTERNOON */
    private static final String CULTURAL = "14";

    @Autowired
    MockMvc mvc;

    @Autowired
    PlaceRepository placeRepository;

    @Autowired
    StationRepository stationRepository;

    @Autowired
    UserService userService;

    @Autowired
    VisitRepository visitRepository;

    @MockitoSpyBean
    LlmClient llmClient;

    @MockitoSpyBean
    PlaceService placeService;

    double baseLat;
    double baseLng;
    Long stationId;

    @BeforeEach
    void setUp() {
        int area = AREA.incrementAndGet();
        baseLat = 35.10;
        baseLng = 128.0 + area * 0.3;
        Station station = Station.of("N" + area);
        station.update("부산테스트역" + area, "테스트선", baseLat, baseLng);
        stationId = stationRepository.save(station).getId();
    }

    /** 역에서 북쪽으로 약 meters 미터 떨어진 곳에 노출 장소를 둔다 */
    private Long savePlace(String name, String contentTypeId, int meters) {
        Place place = Place.fromTour(String.valueOf(CONTENT_ID.incrementAndGet()));
        place.updateTourInfo(contentTypeId, name, "부산", baseLat + meters / METERS_PER_DEGREE, baseLng,
                "https://example.com/" + name + ".jpg", "20261101000000");
        place.updateOverview(name + " 설명");
        place.applyRules(null, true, false);
        return placeRepository.save(place).getId();
    }

    private void visit(String userId, Long placeId, LocalDate date) {
        visitRepository.save(Visit.create(userId, placeId, date));
    }

    private ResultActions now(String weather, String time) throws Exception {
        return mvc.perform(get("/api/now")
                .param("stationId", String.valueOf(stationId))
                .param("weather", weather)
                .param("time", time));
    }

    @Nested
    class 성공 {

        @Test
        void 가장_가까운_후보를_추천하고_상황을_함께_준다() throws Exception {
            Long near = savePlace("가까운공원", OUTDOOR, 200);
            Long far = savePlace("먼공원", OUTDOOR, 500);

            now("CLEAR", "15:00")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultType").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.place.placeId").value(near))
                    .andExpect(jsonPath("$.data.place.name").value("가까운공원"))
                    .andExpect(jsonPath("$.data.place.imageUrl").value("https://example.com/가까운공원.jpg"))
                    .andExpect(jsonPath("$.data.place.distanceMeters").value(200))
                    .andExpect(jsonPath("$.data.place.walkMinutes").value(3))
                    .andExpect(jsonPath("$.data.place.indoor").value(false))
                    .andExpect(jsonPath("$.data.reason").value(startsWith("[MOCK EN]")))
                    .andExpect(jsonPath("$.data.reasonKo").value(startsWith("[MOCK]")))
                    .andExpect(jsonPath("$.data.context.time").value("15:00"))
                    .andExpect(jsonPath("$.data.context.timeSlot").value("AFTERNOON"))
                    .andExpect(jsonPath("$.data.context.weather").value("CLEAR"))
                    .andExpect(jsonPath("$.data.context.weatherSource").value("SIMULATED"))
                    .andExpect(jsonPath("$.data.context.baseStation").value("부산테스트역" + AREA.get()))
                    .andExpect(jsonPath("$.data.alternatives[0].placeId").value(far))
                    .andExpect(jsonPath("$.data.alternatives[0].distanceMeters").value(500))
                    .andExpect(jsonPath("$.data.weeklyPopular").isArray());
        }

        @Test
        void 사용자_언어와_국적으로_이유를_쓴다() throws Exception {
            savePlace("공원", OUTDOOR, 100);
            String userId = TestUsers.create(userService, Nationality.VN, Language.VI);

            mvc.perform(get("/api/now").param("userId", userId).param("stationId", String.valueOf(stationId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.reason").value(startsWith("[MOCK VI] 베트남")));
        }

        @Test
        void 후보마다_내_나라_연결을_AI_입력에_넣는다() throws Exception {
            Long prison = savePlace("감옥", OUTDOOR, 100);
            savePlace("공원", OUTDOOR, 300);
            doReturn(Optional.of(new PlaceConnection("하노이 호아로 수용소", "Nhà tù Hỏa Lò", "감옥을 역사관으로 남긴 곳")))
                    .when(placeService).findConnection(argThat(place -> place.getId().equals(prison)), eq(Nationality.VN));
            String userId = TestUsers.create(userService, Nationality.VN, Language.VI);

            mvc.perform(get("/api/now").param("userId", userId).param("stationId", String.valueOf(stationId))
                            .param("time", "15:00").param("weather", "CLEAR"))
                    .andExpect(status().isOk());

            ArgumentCaptor<String> input = ArgumentCaptor.forClass(String.class);
            verify(llmClient).entity(any(), input.capture(), eq(RecommendChoice.class), any());
            assertThat(input.getValue())
                    .contains("\"connection\":{\"counterpart\":\"하노이 호아로 수용소\",\"localName\":\"Nhà tù Hỏa Lò\"")
                    .contains("\"connection\":null"); // 연결이 없는 후보
        }

        @Test
        void 비가_오면_실내_장소만_추천한다() throws Exception {
            Long outdoor = savePlace("야외", OUTDOOR, 100);
            Long indoor = savePlace("박물관", CULTURAL, 400);

            now("RAINY", "15:00")
                    .andExpect(jsonPath("$.data.place.placeId").value(indoor))
                    .andExpect(jsonPath("$.data.place.indoor").value(true))
                    .andExpect(jsonPath("$.data.context.weather").value("RAINY"));
            now("CLEAR", "15:00")
                    .andExpect(jsonPath("$.data.place.placeId").value(outdoor));
        }

        @Test
        void 지금_시간대에_맞는_장소만_추천한다() throws Exception {
            savePlace("낮에만", CULTURAL, 100);
            Long allDay = savePlace("저녁에도", OUTDOOR, 300);

            now("CLEAR", "18:00")
                    .andExpect(jsonPath("$.data.context.timeSlot").value("EVENING"))
                    .andExpect(jsonPath("$.data.place.placeId").value(allDay));
        }

        @Test
        void 밤에도_시간대_조건_없이_가까운_곳을_추천한다() throws Exception {
            Long dayOnly = savePlace("낮에만", CULTURAL, 100);

            now("CLEAR", "23:30")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.context.timeSlot").value(nullValue()))
                    .andExpect(jsonPath("$.data.place.placeId").value(dayOnly));
        }

        @Test
        void 최근_7일_방문자가_많은_장소를_지난주_인기로_준다() throws Exception {
            Long popular = savePlace("인기장소", OUTDOOR, 100);
            Long second = savePlace("두번째", OUTDOOR, 200);
            Long old = savePlace("오래전", OUTDOOR, 300);
            LocalDate today = SeoulTime.today();
            // 다른 테스트의 방문보다 확실히 많게: 인기장소 9명, 두번째 8명. 같은 사람이 여러 날 와도 1명
            for (int i = 0; i < 9; i++) visit("pop-" + AREA.get() + "-" + i, popular, today.minusDays(i % 7));
            visit("pop-" + AREA.get() + "-0", popular, today.minusDays(3));
            for (int i = 0; i < 8; i++) visit("sec-" + AREA.get() + "-" + i, second, today.minusDays(1));
            // 7일 전(오늘 포함 7일 밖) 방문은 세지 않는다
            for (int i = 0; i < 20; i++) visit("old-" + AREA.get() + "-" + i, old, today.minusDays(7));

            now("CLEAR", "15:00")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.weeklyPopular[0].placeId").value(popular))
                    .andExpect(jsonPath("$.data.weeklyPopular[0].name").value("인기장소"))
                    .andExpect(jsonPath("$.data.weeklyPopular[0].visitorCount").value(9))
                    .andExpect(jsonPath("$.data.weeklyPopular[1].placeId").value(second))
                    .andExpect(jsonPath("$.data.weeklyPopular[1].visitorCount").value(8))
                    .andExpect(jsonPath("$.data.weeklyPopular[*].placeId", not(hasItem(old.intValue()))))
                    .andExpect(jsonPath("$.data.weeklyPopular.length()").value(lessThanOrEqualTo(3)));
        }

        @Test
        void AI가_후보_밖_장소를_고르면_버리고_가장_가까운_후보와_기본_문구() throws Exception {
            Long near = savePlace("가까운곳", OUTDOOR, 100);
            doReturn(new RecommendChoice(-1L, "made up", "지어낸 곳"))
                    .when(llmClient).entity(any(), anyString(), eq(RecommendChoice.class), any());

            now("CLEAR", "15:00")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.place.placeId").value(near))
                    .andExpect(jsonPath("$.data.reason").value("A good place to visit nearby right now."))
                    .andExpect(jsonPath("$.data.reasonKo").value("지금 가까이에서 가 볼 만한 곳이에요."));
        }

        @Test
        void AI_호출이_실패해도_가장_가까운_후보를_준다() throws Exception {
            Long near = savePlace("가까운곳", OUTDOOR, 100);
            doThrow(new LlmException(LlmErrorType.TIMEOUT, "test"))
                    .when(llmClient).entity(any(), anyString(), eq(RecommendChoice.class), any());

            now("CLEAR", "15:00")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.place.placeId").value(near))
                    .andExpect(jsonPath("$.data.reasonKo").value("지금 가까이에서 가 볼 만한 곳이에요."));
        }
    }

    @Nested
    class 실패 {

        @Test
        void 없는_사용자는_404() throws Exception {
            mvc.perform(get("/api/now").param("userId", "no-such-user"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
        }

        @Test
        void 잘못된_날씨나_시각은_400() throws Exception {
            mvc.perform(get("/api/now").param("weather", "SUNNY"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON_INVALID_REQUEST"));
            mvc.perform(get("/api/now").param("time", "25:00"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON_INVALID_REQUEST"));
        }
    }
}
