package com.hackathon.global.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.repository.PlaceRepository;
import com.hackathon.domain.user.service.UserService;
import com.hackathon.global.common.Language;
import com.hackathon.global.common.Nationality;
import com.hackathon.support.TestUsers;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * 다른 기능 API도 토큰을 확인하는지 본다 (UserService.getUser 한 곳에서 확인한다).
 * 다른 기능 테스트는 토큰 검사를 끄고 돌기 때문에 여기서 켜고 대표 API를 확인한다.
 */
@SpringBootTest(properties = {"llm.mock=true", "app.auth.enforce-token=true"})
@AutoConfigureMockMvc
class AuthEnforcementTest {

    private static final AtomicInteger CONTENT_ID = new AtomicInteger(450_000);

    @Autowired
    MockMvc mvc;

    @Autowired
    UserService userService;

    @Autowired
    JwtProvider jwtProvider;

    @Autowired
    PlaceRepository placeRepository;

    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request, String userId) {
        return request.header("Authorization", "Bearer " + jwtProvider.issue(userId).token());
    }

    private Long activePlace() {
        Place place = Place.fromTour(String.valueOf(CONTENT_ID.incrementAndGet()));
        place.updateTourInfo("12", "독립문", "서울 서대문구", 37.57, 126.96, null, "20261101000000");
        place.applyRules(null, true, false);
        return placeRepository.save(place).getId();
    }

    private MockHttpServletRequestBuilder writeReview(Long placeId, String userId) {
        return post("/api/places/{placeId}/reviews", placeId).contentType(MediaType.APPLICATION_JSON).content("""
                {"userId": "%s", "content": "사진 찍기 좋아요", "language": "KO"}
                """.formatted(userId));
    }

    @Test
    void 나의_기록은_본인_토큰이_있어야_본다() throws Exception {
        String me = TestUsers.create(userService, Nationality.VN, Language.VI);
        String other = TestUsers.create(userService, Nationality.JP, Language.JA);

        mvc.perform(get("/api/users/{userId}/visits", me))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("USER_LOGIN_REQUIRED"));
        mvc.perform(as(get("/api/users/{userId}/visits", me), other))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_FORBIDDEN"));
        mvc.perform(as(get("/api/users/{userId}/visits", me), me))
                .andExpect(status().isOk());
    }

    @Test
    void 한걸음은_본인으로만_쓴다() throws Exception {
        String me = TestUsers.create(userService, Nationality.VN, Language.VI);
        String other = TestUsers.create(userService, Nationality.JP, Language.JA);
        Long placeId = activePlace();

        mvc.perform(writeReview(placeId, me))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("USER_LOGIN_REQUIRED"));
        mvc.perform(as(writeReview(placeId, me), other))
                .andExpect(status().isForbidden());
        mvc.perform(as(writeReview(placeId, me), me))
                .andExpect(status().isCreated());
    }

    @Test
    void 추천에_userId를_보내면_토큰이_필요하고_안_보내면_로그인_없이_된다() throws Exception {
        String me = TestUsers.create(userService, Nationality.VN, Language.VI);
        activePlace();

        mvc.perform(get("/api/now").param("userId", me))
                .andExpect(status().isUnauthorized());
        mvc.perform(as(get("/api/now").param("userId", me), me))
                .andExpect(status().isOk());
        mvc.perform(get("/api/now"))
                .andExpect(status().isOk());
    }

    @Test
    void 사용자가_필요_없는_API는_토큰_없이_된다() throws Exception {
        mvc.perform(get("/api/stations")).andExpect(status().isOk());
        mvc.perform(get("/api/map/pins")).andExpect(status().isOk());
    }
}
