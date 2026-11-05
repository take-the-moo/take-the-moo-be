package com.hackathon.domain.user.controller;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hackathon.domain.user.entity.AppUser;
import com.hackathon.domain.user.repository.AppUserRepository;
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
import org.springframework.test.web.servlet.ResultActions;

/** 아이디 중복 확인·회원가입·로그인. 실제 실행처럼 토큰 검사를 켠다 */
@SpringBootTest(properties = {"llm.mock=true", "app.auth.enforce-token=true"})
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    AppUserRepository appUserRepository;

    private ResultActions signUp(String loginId, String password, String name) throws Exception {
        return mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content("""
                {"loginId": "%s", "password": "%s", "name": "%s", "nationality": "VN", "language": "VI"}
                """.formatted(loginId, password, name)));
    }

    private ResultActions login(String loginId, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"loginId": "%s", "password": "%s"}
                """.formatted(loginId, password)));
    }

    @Nested
    class 성공 {

        @Test
        void 가입하면_토큰과_사용자를_주고_그_토큰으로_내_정보를_본다() throws Exception {
            String loginId = TestUsers.newLoginId();

            String body = signUp(loginId, "seodaemun1908", " 린 ")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.resultType").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.accessToken").value(startsWith("eyJ")))
                    .andExpect(jsonPath("$.data.expiresAt").isString())
                    .andExpect(jsonPath("$.data.user.userId").value(loginId))
                    .andExpect(jsonPath("$.data.user.name").value("린"))
                    .andExpect(jsonPath("$.data.user.nationality").value("VN"))
                    .andExpect(jsonPath("$.data.user.language").value("VI"))
                    .andReturn().getResponse().getContentAsString();
            String token = com.jayway.jsonpath.JsonPath.read(body, "$.data.accessToken");

            mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userId").value(loginId));
        }

        @Test
        void 비밀번호는_해시로만_저장한다() throws Exception {
            String loginId = TestUsers.newLoginId();
            signUp(loginId, "seodaemun1908", "린").andExpect(status().isCreated());

            AppUser user = appUserRepository.findById(loginId).orElseThrow();
            org.assertj.core.api.Assertions.assertThat(user.getPasswordHash())
                    .isNotEqualTo("seodaemun1908")
                    .startsWith("$2");
        }

        @Test
        void 로그인하면_토큰을_준다() throws Exception {
            String loginId = TestUsers.newLoginId();
            signUp(loginId, "seodaemun1908", "린");

            login(loginId, "seodaemun1908")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").value(startsWith("eyJ")))
                    .andExpect(jsonPath("$.data.user.userId").value(loginId));
        }

        @Test
        void 아이디_중복_확인() throws Exception {
            String taken = TestUsers.newLoginId();
            signUp(taken, "seodaemun1908", "린");
            String free = TestUsers.newLoginId();

            mvc.perform(get("/api/auth/check-id").param("loginId", taken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.loginId").value(taken))
                    .andExpect(jsonPath("$.data.available").value(false));
            mvc.perform(get("/api/auth/check-id").param("loginId", free))
                    .andExpect(jsonPath("$.data.available").value(true));
        }
    }

    @Nested
    class 실패 {

        @Test
        void 이미_있는_아이디로_가입하면_409() throws Exception {
            String loginId = TestUsers.newLoginId();
            signUp(loginId, "seodaemun1908", "린").andExpect(status().isCreated());

            signUp(loginId, "another-password", "다른사람")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("USER_LOGIN_ID_TAKEN"));
            login(loginId, "seodaemun1908").andExpect(status().isOk()); // 처음 가입한 사람의 비밀번호가 그대로다
        }

        @Test
        void 비밀번호가_틀리거나_없는_아이디면_같은_401() throws Exception {
            String loginId = TestUsers.newLoginId();
            signUp(loginId, "seodaemun1908", "린");

            login(loginId, "wrong-password")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("USER_INVALID_CREDENTIALS"));
            login(TestUsers.newLoginId(), "seodaemun1908")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("USER_INVALID_CREDENTIALS"));
        }

        @Test
        void 샘플_사용자로는_로그인할_수_없다() throws Exception {
            String sample = appUserRepository.save(AppUser.sample(Nationality.VN, Language.VI)).getId();

            login(sample, "anything-123")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("USER_INVALID_CREDENTIALS"));
        }

        @Test
        void 아이디_형식이_틀리면_422() throws Exception {
            signUp("Linh-2026", "seodaemun1908", "린")
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
            mvc.perform(get("/api/auth/check-id").param("loginId", "ab"))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
        }

        @Test
        void 비밀번호가_짧거나_이름이_비면_422() throws Exception {
            signUp(TestUsers.newLoginId(), "short", "린")
                    .andExpect(status().isUnprocessableContent());
            signUp(TestUsers.newLoginId(), "seodaemun1908", " ")
                    .andExpect(status().isUnprocessableContent());
        }

        @Test
        void 없는_국적_코드는_400() throws Exception {
            mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content("""
                            {"loginId": "%s", "password": "seodaemun1908", "name": "린", "nationality": "KR", "language": "VI"}
                            """.formatted(TestUsers.newLoginId())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON_INVALID_REQUEST"));
        }
    }
}
