package com.hackathon.domain.llm.exception;

import com.hackathon.global.response.type.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LlmErrorType implements ErrorCode {

    PROMPT_NOT_FOUND(400, "등록되지 않은 promptKey입니다."),
    CALL_FAILED(502, "AI 응답을 가져오지 못했습니다. 잠시 후 다시 시도해 주세요."),
    INVALID_OUTPUT(502, "AI 응답을 해석하지 못했습니다. 다시 시도해 주세요."),
    TIMEOUT(504, "AI 응답 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요.");

    private final int httpStatusCode;
    private final String message;
}
