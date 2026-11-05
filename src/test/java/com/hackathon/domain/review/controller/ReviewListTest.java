package com.hackathon.domain.review.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.repository.PlaceRepository;
import com.hackathon.domain.review.entity.Review;
import com.hackathon.domain.review.repository.ReviewRepository;
import com.hackathon.domain.review.repository.ReviewTranslationRepository;
import com.hackathon.domain.user.dto.request.UserCreateRequestDto;
import com.hackathon.domain.user.service.UserService;
import com.hackathon.global.common.Language;
import com.hackathon.global.common.Nationality;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** 목 모드: 번역은 "[MOCK 언어] 원문"으로 온다. 한걸음은 검사를 거치지 않고 저장소에 바로 넣는다 */
@SpringBootTest(properties = "llm.mock=true")
@AutoConfigureMockMvc
class ReviewListTest {

    private static final AtomicInteger CONTENT_ID = new AtomicInteger(600_000);

    @Autowired
    MockMvc mvc;

    @Autowired
    PlaceRepository placeRepository;

    @Autowired
    ReviewRepository reviewRepository;

    @Autowired
    ReviewTranslationRepository reviewTranslationRepository;

    @Autowired
    UserService userService;

    Long placeId;
    String vietnamese;
    String japanese;

    @BeforeEach
    void setUp() {
        Place place = Place.fromTour(String.valueOf(CONTENT_ID.incrementAndGet()));
        place.updateTourInfo("12", "독립문", "서울 서대문구", 37.57, 126.96, null, "20261101000000");
        place.applyRules(null, true, false);
        placeId = placeRepository.save(place).getId();
        vietnamese = userService.create(new UserCreateRequestDto(Nationality.VN, Language.VI)).userId();
        japanese = userService.create(new UserCreateRequestDto(Nationality.JP, Language.JA)).userId();

        reviewRepository.save(Review.create(placeId, japanese, "門が思ったより小さかった", Language.JA));
        reviewRepository.save(Review.create(placeId, vietnamese, "Đẹp lắm", Language.VI));
        reviewRepository.save(Review.create(placeId, vietnamese, "아침에 오면 한적해요", Language.KO));
    }

    @Nested
    class 성공 {

        @Test
        void 최신순으로_보는_사람_언어로_번역한다() throws Exception {
            mvc.perform(get("/api/places/{placeId}/reviews", placeId).param("language", "VI"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.placeId").value(placeId))
                    .andExpect(jsonPath("$.data.viewerLanguage").value("VI"))
                    .andExpect(jsonPath("$.data.reviews", hasSize(3)))
                    .andExpect(jsonPath("$.data.reviews[0].content").value("아침에 오면 한적해요"))
                    .andExpect(jsonPath("$.data.reviews[0].translatedContent").value("[MOCK VI] 아침에 오면 한적해요"))
                    .andExpect(jsonPath("$.data.reviews[0].nationality").value("VN"))
                    .andExpect(jsonPath("$.data.reviews[0].writtenDate").isString())
                    .andExpect(jsonPath("$.data.reviews[1].language").value("VI"))
                    .andExpect(jsonPath("$.data.reviews[1].translatedContent").value(nullValue()))
                    .andExpect(jsonPath("$.data.reviews[2].nationality").value("JP"))
                    .andExpect(jsonPath("$.data.reviews[2].translatedContent").value("[MOCK VI] 門が思ったより小さかった"))
                    .andExpect(jsonPath("$.data.reviews[0].userId").doesNotExist())
                    .andExpect(jsonPath("$.data.reviews[0].createdAt").doesNotExist());
        }

        @Test
        void 언어가_없으면_userId의_언어를_쓴다() throws Exception {
            mvc.perform(get("/api/places/{placeId}/reviews", placeId).param("userId", japanese))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.viewerLanguage").value("JA"))
                    .andExpect(jsonPath("$.data.reviews[2].translatedContent").value(nullValue()));
        }

        @Test
        void 둘_다_없으면_한국어() throws Exception {
            mvc.perform(get("/api/places/{placeId}/reviews", placeId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.viewerLanguage").value("KO"))
                    .andExpect(jsonPath("$.data.reviews[0].translatedContent").value(nullValue()));
        }

        @Test
        void 번역은_저장해_두고_다시_쓴다() throws Exception {
            long before = reviewTranslationRepository.count();
            mvc.perform(get("/api/places/{placeId}/reviews", placeId).param("language", "EN")).andExpect(status().isOk());
            long afterFirst = reviewTranslationRepository.count();
            mvc.perform(get("/api/places/{placeId}/reviews", placeId).param("language", "EN")).andExpect(status().isOk());

            assertThat(afterFirst - before).isEqualTo(3);
            assertThat(reviewTranslationRepository.count()).isEqualTo(afterFirst);
        }

        @Test
        void limit만큼_준다() throws Exception {
            mvc.perform(get("/api/places/{placeId}/reviews", placeId).param("limit", "2"))
                    .andExpect(jsonPath("$.data.reviews", hasSize(2)));
        }
    }

    @Nested
    class 실패 {

        @Test
        void 없는_장소는_404() throws Exception {
            mvc.perform(get("/api/places/{placeId}/reviews", 999_999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("PLACE_NOT_FOUND"));
        }

        @Test
        void 없는_사용자는_404() throws Exception {
            mvc.perform(get("/api/places/{placeId}/reviews", placeId).param("userId", "no-such-user"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
        }

        @Test
        void 없는_언어_코드는_400() throws Exception {
            mvc.perform(get("/api/places/{placeId}/reviews", placeId).param("language", "KR"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON_INVALID_REQUEST"));
        }
    }
}
