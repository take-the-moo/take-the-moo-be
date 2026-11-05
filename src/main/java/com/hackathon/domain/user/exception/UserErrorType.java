package com.hackathon.domain.user.exception;

import com.hackathon.global.response.type.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserErrorType implements ErrorCode {

    NOT_FOUND(404, "사용자를 찾을 수 없습니다."),
    LOGIN_ID_TAKEN(409, "이미 사용 중인 아이디입니다."),
    INVALID_CREDENTIALS(401, "아이디 또는 비밀번호가 맞지 않습니다."),
    LOGIN_REQUIRED(401, "로그인이 필요합니다."),
    INVALID_TOKEN(401, "로그인이 만료되었습니다. 다시 로그인해 주세요."),
    FORBIDDEN(403, "다른 사용자의 정보에는 접근할 수 없습니다.");

    private final int httpStatusCode;
    private final String message;
}
