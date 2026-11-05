package com.hackathon.domain.review.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.repository.PlaceRepository;
import com.hackathon.domain.review.repository.ReviewRepository;
import com.hackathon.support.TestUsers;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 목 모드: 규칙 필터(링크·이메일·전화번호)는 동작하고, AI 검사(moderate)는 통과로 본다.
 * AI가 거절하는 경우는 ReviewModerationTest에서 확인한다.
 */
@SpringBootTest(properties = "llm.mock=true")
@AutoConfigureMockMvc
class ReviewControllerTest {

    private static final AtomicInteger CONTENT_ID = new AtomicInteger(800_000);

    @Autowired
    MockMvc mvc;

    @Autowired
    PlaceRepository placeRepository;

    @Autowired
    ReviewRepository reviewRepository;

    @Autowired
    UserService userService;

    Long placeId;
    String userId;

    @BeforeEach
    void setUp() {
        Place place = Place.fromTour(String.valueOf(CONTENT_ID.incrementAndGet()));
        place.updateTourInfo("12", "독립문", "서울 서대문구", 37.57, 126.96, null, "20261101000000");
        place.applyRules(null, true, false);
        placeId = placeRepository.save(place).getId();
        userId = TestUsers.create(userService, Nationality.VN, Language.VI);
    }

    private ResultActions write(Long placeId, String body) throws Exception {
        return mvc.perform(post("/api/places/{placeId}/reviews", placeId)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String body(String content, String language) {
        return """
                {"userId": "%s", "content": "%s"%s}
                """.formatted(userId, content, language == null ? "" : ", \"language\": \"" + language + "\"");
    }

    @Nested
    class 성공 {

        @Test
        void 한걸음을_남긴다() throws Exception {
            write(placeId, body("  아침에 오면 한적해서 사진 찍기 좋아요  ", "KO"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.resultType").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.reviewId").isNumber())
                    .andExpect(jsonPath("$.data.placeId").value(placeId))
                    .andExpect(jsonPath("$.data.content").value("아침에 오면 한적해서 사진 찍기 좋아요"))
                    .andExpect(jsonPath("$.data.language").value("KO"))
                    .andExpect(jsonPath("$.data.nationality").value("VN"))
                    .andExpect(jsonPath("$.data.writtenDate").isString())
                    .andExpect(jsonPath("$.data.userId").doesNotExist())
                    .andExpect(jsonPath("$.data.createdAt").doesNotExist());
        }

        @Test
        void 언어를_비우면_감지한_언어로_저장한다() throws Exception {
            // 목 모드의 감지 결과는 KO로 고정
            write(placeId, body("Đẹp lắm", null))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.language").value("KO"));
        }

        @Test
        void 연도_범위는_전화번호로_보지_않는다() throws Exception {
            write(placeId, body("1908-1987년까지 감옥이었다니 놀라웠어요", "KO"))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    class 실패 {

        @Test
        void 링크가_있으면_422이고_저장하지_않는다() throws Exception {
            long before = reviewRepository.count();
            write(placeId, body("맛집 정보는 www.example.com 에서", "KO"))
                    .andExpect(status().is(422))
                    .andExpect(jsonPath("$.code").value("REVIEW_INAPPROPRIATE"));
            assertThat(reviewRepository.count()).isEqualTo(before);
        }

        @Test
        void 전화번호가_있으면_422() throws Exception {
            write(placeId, body("같이 가실 분 010-1234-5678", "KO"))
                    .andExpect(status().is(422))
                    .andExpect(jsonPath("$.code").value("REVIEW_INAPPROPRIATE"));
        }

        @Test
        void 없는_사용자는_404() throws Exception {
            write(placeId, """
                    {"userId": "no-such-user", "content": "좋아요"}
                    """)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
        }

        @Test
        void 없는_장소는_404() throws Exception {
            write(999_999L, body("좋아요", "KO"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("PLACE_NOT_FOUND"));
        }

        @Test
        void 길이가_넘으면_422() throws Exception {
            write(placeId, body("가".repeat(201), "KO"))
                    .andExpect(status().is(422))
                    .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
        }

        @Test
        void 빈_글은_422() throws Exception {
            write(placeId, body("   ", "KO"))
                    .andExpect(status().is(422))
                    .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
        }
    }
}
