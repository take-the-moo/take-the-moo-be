package com.hackathon.domain.user.exception;

import com.hackathon.global.response.type.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserErrorType implements ErrorCode {

    NOT_FOUND(404, "사용자를 찾을 수 없습니다. 처음 설정을 다시 해 주세요.");

    private final int httpStatusCode;
    private final String message;
}
