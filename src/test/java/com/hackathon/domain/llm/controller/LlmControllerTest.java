package com.hackathon.domain.llm.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 목 모드(llm.mock=true)로 실제 API 호출 없이 엔드포인트별 응답 형식(ApiResult)을 검증한다.
 */
@SpringBootTest(properties = "llm.mock=true")
@AutoConfigureMockMvc
class LlmControllerTest {

    @Autowired
    MockMvc mvc;

    @Nested
    class 성공 {

        @Test
        void health() throws Exception {
            expectSuccess(mvc.perform(get("/api/health")))
                    .andExpect(jsonPath("$.data.status").value("ok"))
                    .andExpect(jsonPath("$.data.mock").value(true));
        }

        @Test
        void chat() throws Exception {
            expectSuccess(mvc.perform(post("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"message": "안녕 {중괄호}", "params": {"target": "학생"}}
                            """)))
                    .andExpect(jsonPath("$.data.answer").value(startsWith("[MOCK]")))
                    .andExpect(jsonPath("$.data.model").value("claude-haiku-5-5"))
                    .andExpect(jsonPath("$.data.usage.inputTokens").isNumber())
                    .andExpect(jsonPath("$.data.usage.outputTokens").isNumber())
                    .andExpect(jsonPath("$.data.latencyMs").isNumber());
        }

        @Test
        void stream() throws Exception {
            MvcResult started = mvc.perform(get("/api/chat/stream").param("message", "안녕"))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            String body = mvc.perform(asyncDispatch(started))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            assertThat(body).contains("event:token", "data:{\"text\":");
            assertThat(body.trim()).contains("event:done");
            assertThat(body).doesNotContain("event:error");
            assertThat(body.lastIndexOf("event:done")).isGreaterThan(body.lastIndexOf("event:token"));
        }

        @Test
        void analyze() throws Exception {
            expectSuccess(mvc.perform(post("/api/analyze")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"input": "회의록: 프론트 로그인 완료, 백엔드 LLM 연동 중"}
                            """)))
                    .andExpect(jsonPath("$.data.summary").isString())
                    .andExpect(jsonPath("$.data.keyPoints", hasSize(3)))
                    .andExpect(jsonPath("$.data.actionItems").isArray());
        }

        @Test
        void image() throws Exception {
            MockMultipartFile file = new MockMultipartFile("file", "cat.png", "image/png", new byte[]{1, 2, 3});

            expectSuccess(mvc.perform(multipart("/api/image").file(file).param("question", "뭐가 보여?")))
                    .andExpect(jsonPath("$.data.answer").value(startsWith("[MOCK]")))
                    .andExpect(jsonPath("$.data.model").value("claude-sonnet-5-5"));
        }
    }

    @Nested
    class 실패 {

        @Test
        void 메시지가_비면_422() throws Exception {
            expectFail(mvc.perform(post("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"message": ""}
                            """)), 422, "COMMON_VALIDATION_FAILED")
                    .andExpect(jsonPath("$.message").value(startsWith("요청 값 검증에 실패했습니다.")));
        }

        @Test
        void 분석_입력이_없으면_422() throws Exception {
            expectFail(mvc.perform(post("/api/analyze")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")), 422, "COMMON_VALIDATION_FAILED");
        }

        @Test
        void 없는_promptKey면_400() throws Exception {
            expectFail(mvc.perform(post("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"promptKey": "nope", "message": "안녕"}
                            """)), 400, "LLM_PROMPT_NOT_FOUND");
        }

        @Test
        void 스트림도_시작_전_오류는_ApiResult() throws Exception {
            expectFail(mvc.perform(get("/api/chat/stream")
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .param("promptKey", "nope")
                    .param("message", "안녕")), 400, "LLM_PROMPT_NOT_FOUND");
        }

        @Test
        void 이미지가_아니면_422() throws Exception {
            MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "hi".getBytes());

            expectFail(mvc.perform(multipart("/api/image").file(file).param("question", "뭐야?")),
                    422, "COMMON_VALIDATION_FAILED");
        }

        @Test
        void 이미지가_5MB를_넘으면_422() throws Exception {
            MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png",
                    new byte[5 * 1024 * 1024 + 1]);

            expectFail(mvc.perform(multipart("/api/image").file(file).param("question", "뭐야?")),
                    422, "COMMON_VALIDATION_FAILED");
        }

        @Test
        void 본문이_JSON이_아니면_400() throws Exception {
            expectFail(mvc.perform(post("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{broken")), 400, "COMMON_INVALID_REQUEST");
        }
    }

    private static ResultActions expectSuccess(ResultActions actions) throws Exception {
        return actions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultType").value("SUCCESS"))
                .andExpect(jsonPath("$.httpStatusCode").value(200))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.message").value("요청에 성공하였습니다."))
                .andExpect(jsonPath("$.data").exists());
    }

    private static ResultActions expectFail(ResultActions actions, int status, String code) throws Exception {
        return actions
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.resultType").value("FAIL"))
                .andExpect(jsonPath("$.httpStatusCode").value(status))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
