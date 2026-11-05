package com.hackathon.domain.user.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = "llm.mock=true")
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    MockMvc mvc;

    private ResultActions createUser(String body) throws Exception {
        return mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Nested
    class 성공 {

        @Test
        void 생성하고_조회한다() throws Exception {
            String body = createUser("""
                    {"nationality": "VN", "language": "VI"}
                    """)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.resultType").value("SUCCESS"))
                    .andExpect(jsonPath("$.httpStatusCode").value(201))
                    .andExpect(jsonPath("$.data.userId").isString())
                    .andExpect(jsonPath("$.data.nationality").value("VN"))
                    .andExpect(jsonPath("$.data.language").value("VI"))
                    .andReturn().getResponse().getContentAsString();
            String userId = JsonPath.read(body, "$.data.userId");

            mvc.perform(get("/api/users/{userId}", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultType").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.userId").value(userId))
                    .andExpect(jsonPath("$.data.nationality").value("VN"))
                    .andExpect(jsonPath("$.data.language").value("VI"));
        }
    }

    @Nested
    class 실패 {

        @Test
        void 없는_사용자는_404() throws Exception {
            mvc.perform(get("/api/users/{userId}", "no-such-user"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.resultType").value("FAIL"))
                    .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
        }

        @Test
        void 언어가_없으면_422() throws Exception {
            createUser("""
                    {"nationality": "VN"}
                    """)
                    .andExpect(status().is(422))
                    .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
        }

        @Test
        void 없는_코드값은_400() throws Exception {
            createUser("""
                    {"nationality": "KR", "language": "VI"}
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON_INVALID_REQUEST"));
        }
    }
}
