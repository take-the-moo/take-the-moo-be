package com.hackathon.domain.user.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hackathon.domain.user.service.UserService;
import com.hackathon.global.auth.JwtProvider;
import com.hackathon.global.common.Language;
import com.hackathon.global.common.Nationality;
import com.hackathon.support.TestUsers;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** 내 정보 조회·수정. 실제 실행처럼 토큰 검사를 켠다 */
@SpringBootTest(properties = {"llm.mock=true", "app.auth.enforce-token=true"})
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserService userService;

    @Autowired
    JwtProvider jwtProvider;

    private static MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token);
    }

    private String tokenOf(String userId) {
        return jwtProvider.issue(userId).token();
    }

    @Nested
    class 성공 {

        @Test
        void 토큰으로_내_정보를_본다() throws Exception {
            String userId = TestUsers.create(userService, Nationality.VN, Language.VI);

            mvc.perform(withToken(get("/api/users/me"), tokenOf(userId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userId").value(userId))
                    .andExpect(jsonPath("$.data.name").value("테스트"))
                    .andExpect(jsonPath("$.data.nationality").value("VN"))
                    .andExpect(jsonPath("$.data.language").value("VI"));
            mvc.perform(withToken(get("/api/users/{userId}", userId), tokenOf(userId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userId").value(userId));
        }

        @Test
        void 보낸_항목만_바꾼다() throws Exception {
            String userId = TestUsers.create(userService, Nationality.VN, Language.VI);
            String token = tokenOf(userId);

            mvc.perform(withToken(patch("/api/users/me"), token).contentType(MediaType.APPLICATION_JSON).content("""
                            {"language": "EN"}
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.language").value("EN"))
                    .andExpect(jsonPath("$.data.nationality").value("VN"))
                    .andExpect(jsonPath("$.data.name").value("테스트"));

            mvc.perform(withToken(patch("/api/users/me"), token).contentType(MediaType.APPLICATION_JSON).content("""
                            {"name": " 린 ", "nationality": "JP"}
                            """))
                    .andExpect(jsonPath("$.data.name").value("린"))
                    .andExpect(jsonPath("$.data.nationality").value("JP"))
                    .andExpect(jsonPath("$.data.language").value("EN"));

            mvc.perform(withToken(get("/api/users/me"), token))
                    .andExpect(jsonPath("$.data.name").value("린"))
                    .andExpect(jsonPath("$.data.nationality").value("JP"))
                    .andExpect(jsonPath("$.data.language").value("EN"));
        }
    }

    @Nested
    class 실패 {

        @Test
        void 토큰이_없으면_401() throws Exception {
            mvc.perform(get("/api/users/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("USER_LOGIN_REQUIRED"));
        }

        @Test
        void 위조된_토큰이면_401() throws Exception {
            String userId = TestUsers.create(userService, Nationality.VN, Language.VI);
            String token = tokenOf(userId);
            String forged = token.substring(0, token.length() - 4) + "AAAA";

            mvc.perform(withToken(get("/api/users/me"), forged))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("USER_INVALID_TOKEN"));
            mvc.perform(get("/api/users/me").header("Authorization", "Basic abc"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("USER_INVALID_TOKEN"));
        }

        @Test
        void 다른_사람_정보는_403() throws Exception {
            String me = TestUsers.create(userService, Nationality.VN, Language.VI);
            String other = TestUsers.create(userService, Nationality.JP, Language.JA);

            mvc.perform(withToken(get("/api/users/{userId}", other), tokenOf(me)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("USER_FORBIDDEN"));
        }

        @Test
        void 이름을_비우거나_너무_길면_422() throws Exception {
            String token = tokenOf(TestUsers.create(userService, Nationality.VN, Language.VI));

            mvc.perform(withToken(patch("/api/users/me"), token).contentType(MediaType.APPLICATION_JSON).content("""
                            {"name": "   "}
                            """))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
            mvc.perform(withToken(patch("/api/users/me"), token).contentType(MediaType.APPLICATION_JSON).content("""
                            {"name": "%s"}
                            """.formatted("가".repeat(21))))
                    .andExpect(status().isUnprocessableContent());
        }

        @Test
        void 없는_국적_코드는_400() throws Exception {
            String token = tokenOf(TestUsers.create(userService, Nationality.VN, Language.VI));

            mvc.perform(withToken(patch("/api/users/me"), token).contentType(MediaType.APPLICATION_JSON).content("""
                            {"nationality": "KR"}
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON_INVALID_REQUEST"));
        }
    }
}
