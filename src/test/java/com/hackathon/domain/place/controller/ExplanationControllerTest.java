package com.hackathon.domain.place.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hackathon.domain.llm.service.LlmClient;
import com.hackathon.domain.place.curation.PlaceCuration;
import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.repository.PlaceRepository;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 목 모드로 해설 번역·캐싱을 검증한다. 목 번역은 "[번역] " + 원문이다.
 * LlmClient는 spy로 감싸 AI 호출 횟수를 센다 (목 모드라 실제 호출은 없다).
 */
@SpringBootTest(properties = "llm.mock=true")
@AutoConfigureMockMvc
class ExplanationControllerTest {

    private static final AtomicInteger CONTENT_ID = new AtomicInteger(200_000);

    @Autowired
    MockMvc mvc;

    @Autowired
    PlaceRepository placeRepository;

    @MockitoSpyBean
    LlmClient llmClient;

    @BeforeEach
    void resetCalls() {
        clearInvocations(llmClient);
    }

    private Place savePlace(String overview, String curatedExplanation, boolean active) {
        Place place = Place.fromTour(String.valueOf(CONTENT_ID.incrementAndGet()));
        place.updateTourInfo("14", "서대문형무소역사관", "서울 서대문구", 37.57, 126.95, null, "20261101000000");
        place.updateOverview(overview);
        PlaceCuration curation = new PlaceCuration(place.getContentId(), "서대문형무소역사관", null, null, true,
                curatedExplanation);
        place.applyRules(curation, active, !active);
        return placeRepository.save(place);
    }

    private ResultActions explain(Long placeId, String language) throws Exception {
        return mvc.perform(post("/api/explanations").contentType(MediaType.APPLICATION_JSON).content("""
                {"placeId": %d, "language": "%s"}
                """.formatted(placeId, language)));
    }

    @Nested
    class 성공 {

        @Test
        void 번역하고_두번째는_저장된_번역을_쓴다() throws Exception {
            Place place = savePlace("TourAPI 개요", "1908년 일제가 세운 감옥이다.", true);

            explain(place.getId(), "VI")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultType").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.placeId").value(place.getId()))
                    .andExpect(jsonPath("$.data.name").value("서대문형무소역사관"))
                    .andExpect(jsonPath("$.data.language").value("VI"))
                    .andExpect(jsonPath("$.data.text").value("[번역] 1908년 일제가 세운 감옥이다."))
                    .andExpect(jsonPath("$.data.original").value("1908년 일제가 세운 감옥이다."))
                    .andExpect(jsonPath("$.data.cached").value(false));

            explain(place.getId(), "VI")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.text").value("[번역] 1908년 일제가 세운 감옥이다."))
                    .andExpect(jsonPath("$.data.cached").value(true));

            verify(llmClient, times(1)).entity(any(), any(), any(), any());
        }

        @Test
        void 언어가_다르면_따로_번역한다() throws Exception {
            Place place = savePlace("개요", null, true);

            explain(place.getId(), "VI").andExpect(jsonPath("$.data.cached").value(false));
            explain(place.getId(), "JA").andExpect(jsonPath("$.data.cached").value(false));
            explain(place.getId(), "JA").andExpect(jsonPath("$.data.cached").value(true));
        }

        @Test
        void 한국어면_AI_없이_원문을_준다() throws Exception {
            Place place = savePlace("개요 원문", null, true);

            explain(place.getId(), "KO")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.text").value("개요 원문"))
                    .andExpect(jsonPath("$.data.original").value("개요 원문"))
                    .andExpect(jsonPath("$.data.cached").value(false));

            verify(llmClient, never()).entity(any(), any(), any(), any());
        }

        @Test
        void 원문이_바뀌면_다시_번역한다() throws Exception {
            Place place = savePlace("처음 개요", null, true);
            explain(place.getId(), "EN").andExpect(jsonPath("$.data.cached").value(false));

            place.updateOverview("고친 개요");
            placeRepository.save(place);

            explain(place.getId(), "EN")
                    .andExpect(jsonPath("$.data.text").value("[번역] 고친 개요"))
                    .andExpect(jsonPath("$.data.cached").value(false));
            explain(place.getId(), "EN").andExpect(jsonPath("$.data.cached").value(true));
        }
    }

    @Nested
    class 실패 {

        @Test
        void 해설_원문이_없으면_422() throws Exception {
            Place place = savePlace(null, null, true);

            explain(place.getId(), "VI")
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("PLACE_EXPLANATION_MISSING"));
        }

        @Test
        void 노출하지_않는_장소는_404() throws Exception {
            Place place = savePlace("개요", null, false);

            explain(place.getId(), "VI")
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("PLACE_NOT_FOUND"));
        }

        @Test
        void 언어가_없으면_422() throws Exception {
            mvc.perform(post("/api/explanations").contentType(MediaType.APPLICATION_JSON).content("""
                            {"placeId": 1}
                            """))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
        }

        @Test
        void 없는_언어_코드는_400() throws Exception {
            explain(1L, "FR")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON_INVALID_REQUEST"));
        }
    }
}
