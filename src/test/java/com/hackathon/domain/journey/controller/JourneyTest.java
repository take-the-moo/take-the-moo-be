package com.hackathon.domain.journey.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hackathon.domain.journey.dto.response.JourneyStoryText;
import com.hackathon.domain.journey.repository.JourneyStoryRepository;
import com.hackathon.domain.llm.exception.LlmErrorType;
import com.hackathon.domain.llm.exception.LlmException;
import com.hackathon.domain.llm.service.LlmClient;
import com.hackathon.domain.photo.entity.Photo;
import com.hackathon.domain.photo.repository.PhotoRepository;
import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.repository.PlaceRepository;
import com.hackathon.domain.review.entity.Review;
import com.hackathon.domain.review.repository.ReviewRepository;
import com.hackathon.domain.user.entity.AppUser;
import com.hackathon.domain.user.repository.AppUserRepository;
import com.hackathon.domain.user.service.UserService;
import com.hackathon.domain.visit.entity.Visit;
import com.hackathon.domain.visit.repository.VisitRepository;
import com.hackathon.global.common.Language;
import com.hackathon.global.common.Nationality;
import com.hackathon.global.external.ExternalApiErrorType;
import com.hackathon.global.external.ExternalApiException;
import com.hackathon.global.external.ExternalHttp;
import com.hackathon.global.external.ExternalImage;
import com.hackathon.global.util.SeoulTime;
import com.hackathon.support.TestUsers;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** 실제 data/courses.json으로 검증한다. 장소를 다른 테스트와 나누지 않으려고 메모리 DB를 따로 쓴다 */
@SpringBootTest(properties = {
        "llm.mock=true",
        "spring.datasource.url=jdbc:h2:mem:journey;MODE=MySQL;DATABASE_TO_LOWER=TRUE"
})
@AutoConfigureMockMvc
class JourneyTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    PlaceRepository placeRepository;

    @Autowired
    VisitRepository visitRepository;

    @Autowired
    ReviewRepository reviewRepository;

    @Autowired
    PhotoRepository photoRepository;

    @Autowired
    JourneyStoryRepository journeyStoryRepository;

    @Autowired
    AppUserRepository appUserRepository;

    @Autowired
    UserService userService;

    @MockitoSpyBean
    LlmClient llmClient;

    @MockitoBean
    ExternalHttp externalHttp;

    Place prison;
    Place gate;
    Place market;
    String userId;
    LocalDate today;

    @BeforeEach
    void setUp() {
        prison = upsert("130152", "서대문형무소역사관", "http://tong.visitkorea.or.kr/prison.jpg");
        gate = upsert("127425", "독립문", "https://tong.visitkorea.or.kr/gate.jpg");
        market = upsert("2789102", "독립문영천시장", null);
        userId = TestUsers.create(userService, Nationality.VN, Language.VI);
        today = SeoulTime.today();
    }

    private Place upsert(String contentId, String name, String imageUrl) {
        Place place = placeRepository.findByContentId(contentId).orElseGet(() -> Place.fromTour(contentId));
        place.updateTourInfo("12", name, "서울 서대문구", 37.57, 126.96, imageUrl, "20261101000000");
        place.applyRules(null, true, false);
        return placeRepository.save(place);
    }

    private void visit(Place place, int daysAgo) {
        visitRepository.save(Visit.create(userId, place.getId(), today.minusDays(daysAgo)));
    }

    private void review(Place place, int daysAgo, String content, String photoId) {
        Review review = reviewRepository.save(Review.create(place.getId(), userId, content, Language.VI, photoId));
        reviewRepository.backdate(review.getId(), SeoulTime.now().minusDays(daysAgo));
    }

    private Photo photo(Place place) {
        return photoRepository.save(Photo.create(userId, place.getId(), "stored.jpg", "image/jpeg", 1));
    }

    private ResultActions journey() throws Exception {
        return mvc.perform(get("/api/users/{userId}/journey", userId)).andExpect(status().isOk());
    }

    private ResultActions story() throws Exception {
        return mvc.perform(get("/api/users/{userId}/journey/story", userId)).andExpect(status().isOk());
    }

    private void verifyAiCalls(int count) {
        verify(llmClient, times(count)).entity(any(), anyString(), eq(JourneyStoryText.class), any());
    }

    private void failAi() {
        doThrow(new LlmException(LlmErrorType.CALL_FAILED, "테스트"))
                .when(llmClient).entity(any(), anyString(), eq(JourneyStoryText.class), any());
    }

    @Test
    void 기록이_없으면_비어_있고_AI를_부르지_않는다() throws Exception {
        journey()
                .andExpect(jsonPath("$.data.startDate").value(nullValue()))
                .andExpect(jsonPath("$.data.endDate").value(nullValue()))
                .andExpect(jsonPath("$.data.stats.stampCount").value(0))
                .andExpect(jsonPath("$.data.moments", hasSize(0)));
        story().andExpect(jsonPath("$.data.story").value(nullValue()));
        verify(llmClient, never()).entity(any(), anyString(), eq(JourneyStoryText.class), any());
    }

    @Test
    void 없는_사용자는_404() throws Exception {
        mvc.perform(get("/api/users/{userId}/journey", "없는-사용자"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
        mvc.perform(get("/api/users/{userId}/journey/story", "없는-사용자"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void 숫자는_나의_기록과_같고_기간은_도장과_한걸음을_함께_본다() throws Exception {
        review(gate, 30, "처음 쓴 글", null);
        visit(prison, 20);
        visit(gate, 15);
        visit(market, 10);
        // 다시 방문해도 도장은 장소당 하나다
        visit(prison, 3);

        journey()
                .andExpect(jsonPath("$.data.startDate").value(today.minusDays(30).toString()))
                .andExpect(jsonPath("$.data.endDate").value(today.minusDays(3).toString()))
                .andExpect(jsonPath("$.data.stats.stampCount").value(3))
                .andExpect(jsonPath("$.data.stats.completedCourseCount").value(1))
                .andExpect(jsonPath("$.data.stats.reviewCount").value(1));
        mvc.perform(get("/api/users/{userId}/visits", userId))
                .andExpect(jsonPath("$.data.stampCount").value(3))
                .andExpect(jsonPath("$.data.completedCourseCount").value(1));
    }

    @Test
    void 카드는_날짜순이고_같은_날은_첫_도장_한걸음_코스_완주_순서다() throws Exception {
        visit(prison, 20);
        visit(gate, 20);
        visit(market, 5);
        // 재방문해도 완주 날짜는 바뀌지 않는다
        visit(market, 1);
        review(market, 5, "시장 글", null);
        review(prison, 20, "역사관 글", null);
        review(gate, 2, "독립문 글", null);

        journey()
                .andExpect(jsonPath("$.data.moments[*].type", contains(
                        "FIRST_STAMP", "REVIEW", "REVIEW", "COURSE_COMPLETED", "REVIEW")))
                .andExpect(jsonPath("$.data.moments[0].date").value(today.minusDays(20).toString()))
                .andExpect(jsonPath("$.data.moments[0].placeId").value(prison.getId()))
                .andExpect(jsonPath("$.data.moments[1].content").value("역사관 글"))
                .andExpect(jsonPath("$.data.moments[3].date").value(today.minusDays(5).toString()))
                .andExpect(jsonPath("$.data.moments[3].courseName").value("Hành trình Độc lập"))
                .andExpect(jsonPath("$.data.moments[3].placeName").value("독립문영천시장"))
                .andExpect(jsonPath("$.data.moments[3].content").value(nullValue()))
                .andExpect(jsonPath("$.data.moments[4].content").value("독립문 글"));
    }

    @Test
    void 이미지는_한걸음_사진_내_사진_장소_사진_순서다() throws Exception {
        Photo reviewPhoto = photo(gate);
        Photo prisonPhoto = photo(prison);
        review(gate, 4, "한걸음 사진", reviewPhoto.getId());
        review(prison, 3, "내 사진", null);
        review(gate, 2, "내 사진 (독립문)", null);
        review(market, 1, "사진 없음", null);

        journey()
                .andExpect(jsonPath("$.data.moments[0].imageUrl").value("/api/photos/" + reviewPhoto.getId()))
                .andExpect(jsonPath("$.data.moments[1].imageUrl").value("/api/photos/" + prisonPhoto.getId()))
                .andExpect(jsonPath("$.data.moments[2].imageUrl").value("/api/photos/" + reviewPhoto.getId()))
                .andExpect(jsonPath("$.data.moments[3].imageUrl").value(nullValue()));
    }

    @Test
    void 내_사진이_없으면_장소_사진을_서버_주소로_준다() throws Exception {
        visit(prison, 2);

        journey().andExpect(jsonPath("$.data.moments[0].imageUrl").value("/api/places/" + prison.getId() + "/image"));
    }

    @Test
    void 찾을_수_없는_장소의_기록은_뺀다() throws Exception {
        visitRepository.save(Visit.create(userId, 99_999_999L, today.minusDays(9)));
        reviewRepository.save(Review.create(99_999_999L, userId, "사라진 장소", Language.VI, null));
        visit(gate, 1);

        journey()
                .andExpect(jsonPath("$.data.startDate").value(today.minusDays(1).toString()))
                .andExpect(jsonPath("$.data.stats.stampCount").value(1))
                .andExpect(jsonPath("$.data.stats.reviewCount").value(0))
                .andExpect(jsonPath("$.data.moments", hasSize(1)));
    }

    @Test
    void 이야기는_저장해_다시_쓰고_기록이_바뀌면_다시_만든다() throws Exception {
        visit(prison, 2);

        story()
                .andExpect(jsonPath("$.data.story.storyNative", startsWith("[MOCK]")))
                .andExpect(jsonPath("$.data.story.storyEasyKorean", startsWith("[MOCK]")));
        story().andExpect(jsonPath("$.data.story.storyNative", startsWith("[MOCK]")));
        verifyAiCalls(1);

        review(prison, 1, "새 한걸음", null);
        story().andExpect(jsonPath("$.data.story.storyEasyKorean").value(
                "[MOCK] 서대문에서 1곳을 걸었어요. 한걸음을 1개 남겼어요."));
        verifyAiCalls(2);
        assertThat(journeyStoryRepository.findByUserIdAndLanguage(userId, Language.VI)).isPresent();
    }

    @Test
    void 언어를_바꾸면_그_언어의_글을_따로_만든다() throws Exception {
        visit(prison, 2);
        story();

        AppUser user = appUserRepository.findById(userId).orElseThrow();
        user.updateProfile(null, null, Language.JA);
        appUserRepository.save(user);
        story().andExpect(jsonPath("$.data.story.storyNative", startsWith("[MOCK]")));

        verifyAiCalls(2);
        assertThat(journeyStoryRepository.findByUserIdAndLanguage(userId, Language.VI)).isPresent();
        assertThat(journeyStoryRepository.findByUserIdAndLanguage(userId, Language.JA)).isPresent();
    }

    @Test
    void AI가_실패하면_저장된_글이_없을_때_null이다() throws Exception {
        visit(prison, 2);
        failAi();

        story().andExpect(jsonPath("$.data.story").value(nullValue()));
        assertThat(journeyStoryRepository.findByUserIdAndLanguage(userId, Language.VI)).isEmpty();
    }

    @Test
    void AI가_실패하면_이전_글을_준다() throws Exception {
        visit(prison, 2);
        String before = story().andReturn().getResponse().getContentAsString();

        review(prison, 1, "새 한걸음", null);
        failAi();

        assertThat(story().andReturn().getResponse().getContentAsString()).isEqualTo(before);
    }

    @Test
    void 장소_사진은_저장된_주소를_https로_받아_이미지로_준다() throws Exception {
        when(externalHttp.getImage(anyString(), eq("https://tong.visitkorea.or.kr/prison.jpg"), anyInt()))
                .thenReturn(new ExternalImage(new byte[]{1, 2, 3}, "image/jpeg"));

        mvc.perform(get("/api/places/{placeId}/image", prison.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void 장소_사진이_없거나_받지_못하면_오류다() throws Exception {
        mvc.perform(get("/api/places/{placeId}/image", 99_999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLACE_NOT_FOUND"));
        mvc.perform(get("/api/places/{placeId}/image", market.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLACE_IMAGE_NOT_FOUND"));

        when(externalHttp.getImage(anyString(), anyString(), anyInt()))
                .thenThrow(new ExternalApiException(ExternalApiErrorType.CALL_FAILED, "테스트"));
        mvc.perform(get("/api/places/{placeId}/image", gate.getId()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("EXTERNAL_API_CALL_FAILED"));
    }
}
