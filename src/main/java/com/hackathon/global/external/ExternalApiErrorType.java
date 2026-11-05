package com.hackathon.global.external;

import com.hackathon.global.response.type.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExternalApiErrorType implements ErrorCode {

    CALL_FAILED(502, "외부 데이터를 가져오지 못했습니다. 잠시 후 다시 시도해 주세요."),
    INVALID_RESPONSE(502, "외부 데이터 형식을 해석하지 못했습니다.");

    private final int httpStatusCode;
    private final String message;
}
