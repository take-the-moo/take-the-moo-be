package com.hackathon.domain.place.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.entity.Station;
import com.hackathon.domain.place.repository.PlaceRepository;
import com.hackathon.domain.place.repository.StationRepository;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * H2는 테스트 컨텍스트끼리 공유되고 다른 테스트의 장소는 서대문(37.57, 126.96)에 있다.
 * 이 테스트는 테스트마다 서로 다른 먼 곳(울릉도 근처, 경도를 테스트마다 옮긴다)에 장소를 두어 결과가 섞이지 않게 한다.
 */
@SpringBootTest(properties = "llm.mock=true")
@AutoConfigureMockMvc
class NearbyPlaceTest {

    private static final AtomicInteger CONTENT_ID = new AtomicInteger(300_000);
    private static final AtomicInteger AREA = new AtomicInteger();
    /** 위도 1도의 거리 (GeoUtils와 같은 지구 반지름 6,371km) */
    private static final double METERS_PER_DEGREE = 6_371_000 * Math.PI / 180;

    @Autowired
    MockMvc mvc;

    @Autowired
    PlaceRepository placeRepository;

    @Autowired
    StationRepository stationRepository;

    /** 이번 테스트의 기준점 */
    double baseLat;
    double baseLng;

    @BeforeEach
    void setUp() {
        baseLat = 37.48;
        baseLng = 130.0 + AREA.incrementAndGet() * 0.5;
    }

    /** 기준점에서 북쪽으로 약 meters 미터 떨어진 곳에 장소를 둔다 */
    private Long savePlace(String name, int meters, boolean active, String overview) {
        Place place = Place.fromTour(String.valueOf(CONTENT_ID.incrementAndGet()));
        place.updateTourInfo("12", name, "경북 울릉군", baseLat + meters / METERS_PER_DEGREE, baseLng,
                "https://example.com/" + name + ".jpg", "20261101000000");
        place.updateOverview(overview);
        place.applyRules(null, active, false);
        return placeRepository.save(place).getId();
    }

    @Nested
    class 성공 {

        @Test
        void 해설이_있는_노출_장소만_가까운_순으로_limit개() throws Exception {
            Long near = savePlace("가까운곳", 100, true, "해설");
            savePlace("해설없는곳", 200, true, null);
            Long middle = savePlace("중간", 300, true, "해설");
            savePlace("숨긴곳", 50, false, "해설");
            savePlace("먼곳", 400, true, "해설");

            mvc.perform(get("/api/places/nearby")
                            .param("lat", String.valueOf(baseLat))
                            .param("lng", String.valueOf(baseLng))
                            .param("limit", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultType").value("SUCCESS"))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].placeId").value(near))
                    .andExpect(jsonPath("$.data[0].name").value("가까운곳"))
                    .andExpect(jsonPath("$.data[0].imageUrl").value("https://example.com/가까운곳.jpg"))
                    .andExpect(jsonPath("$.data[0].distanceMeters").value(100))
                    .andExpect(jsonPath("$.data[1].placeId").value(middle))
                    .andExpect(jsonPath("$.data[1].distanceMeters").value(300));
        }

        @Test
        void 역을_고르면_그_역에서_가까운_순() throws Exception {
            Long near = savePlace("역앞", 150, true, "해설");
            Station station = Station.of("T7" + AREA.get());
            station.update("울릉테스트역", "테스트선", baseLat, baseLng);
            Long stationId = stationRepository.save(station).getId();

            mvc.perform(get("/api/places/nearby").param("stationId", String.valueOf(stationId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].placeId").value(near))
                    .andExpect(jsonPath("$.data[0].distanceMeters").value(150));
        }

        @Test
        void 위치가_없으면_기본_역_기준이고_기본_5개() throws Exception {
            mvc.perform(get("/api/places/nearby"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(lessThanOrEqualTo(5)));
        }
    }

    @Nested
    class 실패 {

        @Test
        void 위도만_보내면_422() throws Exception {
            mvc.perform(get("/api/places/nearby").param("lat", "37.57"))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("STATION_INVALID_LOCATION"));
        }

        @Test
        void 없는_역이면_404() throws Exception {
            mvc.perform(get("/api/places/nearby").param("stationId", "-1"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("STATION_NOT_FOUND"));
        }

        @Test
        void 숫자가_아닌_위도는_400() throws Exception {
            mvc.perform(get("/api/places/nearby").param("lat", "abc").param("lng", "126.9"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON_INVALID_REQUEST"));
        }
    }
}
