package com.hackathon.domain.scene.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hackathon.domain.place.curation.PlaceCuration;
import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.repository.PlaceRepository;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 목 모드로 장면 생성·캐싱·국적 전환을 검증한다. 장소는 테스트마다 새로 넣는다 (TourAPI 키가 없어 동기화는 건너뛴다).
 */
@SpringBootTest(properties = "llm.mock=true")
@AutoConfigureMockMvc
class SceneControllerTest {

    private static final AtomicInteger CONTENT_ID = new AtomicInteger(900_000);

    @Autowired
    MockMvc mvc;

    @Autowired
    PlaceRepository placeRepository;

    private Place savePlace(String name, String overview, boolean active) {
        Place place = Place.fromTour(String.valueOf(CONTENT_ID.incrementAndGet()));
        place.updateTourInfo("14", name, "서울 서대문구", 37.57, 126.95, null, "20261101000000");
        place.updateOverview(overview);
        PlaceCuration curation = new PlaceCuration(place.getContentId(), name, null, null, true, null);
        place.applyRules(curation, active, !active);
        return placeRepository.save(place);
    }

    private ResultActions requestScene(Long placeId, String nationality, String language) throws Exception {
        return mvc.perform(post("/api/scenes").contentType(MediaType.APPLICATION_JSON).content("""
                {"placeId": %d, "nationality": "%s", "language": "%s"}
                """.formatted(placeId, nationality, language)));
    }

    @Nested
    class 성공 {

        @Test
        void 장면을_만들고_두번째는_캐시로_준다() throws Exception {
            Place place = savePlace("서대문형무소역사관", "1908년에 세워진 감옥 자리에 있는 역사관이다.", true);

            requestScene(place.getId(), "VN", "VI")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultType").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.placeId").value(place.getId()))
                    .andExpect(jsonPath("$.data.placeName").value("서대문형무소역사관"))
                    .andExpect(jsonPath("$.data.nationality").value("VN"))
                    .andExpect(jsonPath("$.data.language").value("VI"))
                    .andExpect(jsonPath("$.data.storyNative").value(startsWith("[MOCK]")))
                    .andExpect(jsonPath("$.data.storyEasyKorean").value(startsWith("[MOCK]")))
                    .andExpect(jsonPath("$.data.connectionLine").value(containsString("베트남")))
                    .andExpect(jsonPath("$.data.cached").value(false));

            requestScene(place.getId(), "VN", "VI")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.cached").value(true));
        }

        @Test
        void 국적을_바꾸면_다른_이야기() throws Exception {
            Place place = savePlace("독립문", "1897년에 세운 문이다.", true);
            requestScene(place.getId(), "VN", "VI").andExpect(jsonPath("$.data.cached").value(false));

            requestScene(place.getId(), "CN", "VI")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.nationality").value("CN"))
                    .andExpect(jsonPath("$.data.connectionLine").value(containsString("중국")))
                    .andExpect(jsonPath("$.data.cached").value(false));
        }
    }

    @Nested
    class 실패 {

        @Test
        void 없는_장소는_404() throws Exception {
            requestScene(999_999L, "VN", "VI")
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("PLACE_NOT_FOUND"));
        }

        @Test
        void 노출하지_않는_장소는_404() throws Exception {
            Place place = savePlace("숨긴 장소", "개요", false);
            requestScene(place.getId(), "VN", "VI")
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("PLACE_NOT_FOUND"));
        }

        @Test
        void 설명이_없는_장소는_422() throws Exception {
            Place place = savePlace("설명 없는 장소", null, true);
            requestScene(place.getId(), "VN", "VI")
                    .andExpect(status().is(422))
                    .andExpect(jsonPath("$.code").value("SCENE_SOURCE_MISSING"));
        }

        @Test
        void 국적이_없으면_422() throws Exception {
            mvc.perform(post("/api/scenes").contentType(MediaType.APPLICATION_JSON).content("""
                            {"placeId": 1, "language": "VI"}
                            """))
                    .andExpect(status().is(422))
                    .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
        }
    }
}
