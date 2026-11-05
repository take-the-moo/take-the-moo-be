package com.hackathon.global.response.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommonErrorType implements ErrorCode {

    INVALID_REQUEST(400, "요청 형식이 올바르지 않습니다."),
    NOT_FOUND(404, "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(405, "지원하지 않는 HTTP 메서드입니다."),
    UNSUPPORTED_MEDIA_TYPE(415, "지원하지 않는 요청 형식입니다."),
    VALIDATION_FAILED(422, "요청 값 검증에 실패하였습니다."),
    INTERNAL_ERROR(500, "서버 내부 오류가 발생했습니다.");

    private final int httpStatusCode;
    private final String message;
}
