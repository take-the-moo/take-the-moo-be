package com.hackathon.domain.review.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hackathon.domain.llm.service.LlmClient;
import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.repository.PlaceRepository;
import com.hackathon.domain.review.dto.response.ModerationResult;
import com.hackathon.domain.review.repository.ReviewRepository;
import com.hackathon.support.TestUsers;
import com.hackathon.domain.user.service.UserService;
import com.hackathon.global.common.Language;
import com.hackathon.global.common.Nationality;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** AI 검사(moderate) 결과에 따른 처리. LlmClient를 대신해 원하는 판정을 돌려준다 */
@SpringBootTest(properties = "llm.mock=true")
@AutoConfigureMockMvc
class ReviewModerationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    PlaceRepository placeRepository;

    @Autowired
    ReviewRepository reviewRepository;

    @Autowired
    UserService userService;

    @MockitoBean
    LlmClient llmClient;

    @SuppressWarnings("unchecked")
    private void moderationReturns(ModerationResult result) {
        when(llmClient.entity(any(), anyString(), eq(ModerationResult.class), any(Supplier.class))).thenReturn(result);
    }

    private Long savePlace(String contentId) {
        Place place = Place.fromTour(contentId);
        place.updateTourInfo("12", "독립문", "서울 서대문구", 37.57, 126.96, null, "20261101000000");
        place.applyRules(null, true, false);
        return placeRepository.save(place).getId();
    }

    private String userId() {
        return TestUsers.create(userService, Nationality.JP, Language.JA);
    }

    @Test
    void AI가_거절하면_422이고_저장하지_않는다() throws Exception {
        moderationReturns(new ModerationResult(false, "HATE", Language.KO));
        Long placeId = savePlace("700001");
        long before = reviewRepository.count();

        mvc.perform(post("/api/places/{placeId}/reviews", placeId).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": "%s", "content": "검사에서 걸리는 글"}
                                """.formatted(userId())))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("REVIEW_INAPPROPRIATE"));
        assertThat(reviewRepository.count()).isEqualTo(before);
    }

    @Test
    void 언어를_비우면_AI가_감지한_언어를_쓴다() throws Exception {
        moderationReturns(new ModerationResult(true, "OK", Language.VI));
        Long placeId = savePlace("700002");

        mvc.perform(post("/api/places/{placeId}/reviews", placeId).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": "%s", "content": "Đẹp lắm"}
                                """.formatted(userId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.language").value("VI"))
                .andExpect(jsonPath("$.data.nationality").value("JP"));
    }

    @Test
    void 요청한_언어가_감지보다_우선한다() throws Exception {
        moderationReturns(new ModerationResult(true, "OK", Language.KO));
        Long placeId = savePlace("700003");

        mvc.perform(post("/api/places/{placeId}/reviews", placeId).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": "%s", "content": "きれいでした", "language": "JA"}
                                """.formatted(userId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.language").value("JA"));
    }
}
