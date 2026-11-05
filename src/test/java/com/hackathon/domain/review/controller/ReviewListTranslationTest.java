package com.hackathon.domain.review.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hackathon.domain.llm.exception.LlmErrorType;
import com.hackathon.domain.llm.exception.LlmException;
import com.hackathon.domain.llm.service.LlmClient;
import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.repository.PlaceRepository;
import com.hackathon.domain.review.dto.response.ReviewTranslationBatch;
import com.hackathon.domain.review.entity.Review;
import com.hackathon.domain.review.repository.ReviewRepository;
import com.hackathon.support.TestUsers;
import com.hackathon.domain.user.service.UserService;
import com.hackathon.global.common.Language;
import com.hackathon.global.common.Nationality;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** 번역 AI 호출 횟수와 실패 처리. LlmClient를 대신해 원하는 결과를 돌려준다 */
@SpringBootTest(properties = "llm.mock=true")
@AutoConfigureMockMvc
class ReviewListTranslationTest {

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

    private Long placeWithKoreanReviews(String contentId, int count) {
        Place place = Place.fromTour(contentId);
        place.updateTourInfo("12", "영천시장", "서울 서대문구", 37.57, 126.96, null, "20261101000000");
        place.applyRules(null, true, false);
        Long placeId = placeRepository.save(place).getId();
        String userId = TestUsers.create(userService, Nationality.CN, Language.ZH);
        for (int i = 0; i < count; i++) reviewRepository.save(Review.create(placeId, userId, "맛있어요 " + i, Language.KO));
        return placeId;
    }

    @Test
    @SuppressWarnings("unchecked")
    void 여러_개를_한_번에_번역하고_두번째는_AI를_부르지_않는다() throws Exception {
        Long placeId = placeWithKoreanReviews("500001", 3);
        when(llmClient.entity(any(), anyString(), eq(ReviewTranslationBatch.class), any(Supplier.class)))
                .thenReturn(new ReviewTranslationBatch(List.of("t2", "t1", "t0")));

        mvc.perform(get("/api/places/{placeId}/reviews", placeId).param("language", "ZH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviews[0].translatedContent").value("t2"));
        mvc.perform(get("/api/places/{placeId}/reviews", placeId).param("language", "ZH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviews[2].translatedContent").value("t0"));

        verify(llmClient, times(1)).entity(any(), anyString(), eq(ReviewTranslationBatch.class), any(Supplier.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void 번역이_실패해도_원문으로_목록을_준다() throws Exception {
        Long placeId = placeWithKoreanReviews("500002", 2);
        when(llmClient.entity(any(), anyString(), eq(ReviewTranslationBatch.class), any(Supplier.class)))
                .thenThrow(new LlmException(LlmErrorType.TIMEOUT, "test"));

        mvc.perform(get("/api/places/{placeId}/reviews", placeId).param("language", "EN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviews[0].content").value("맛있어요 1"))
                .andExpect(jsonPath("$.data.reviews[0].translatedContent").value(nullValue()));
    }
}
