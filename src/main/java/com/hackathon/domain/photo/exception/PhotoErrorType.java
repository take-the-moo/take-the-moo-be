package com.hackathon.domain.photo.exception;

import com.hackathon.global.response.type.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PhotoErrorType implements ErrorCode {

    FORBIDDEN(403, "내가 올린 사진만 쓸 수 있습니다."),
    NOT_FOUND(404, "사진을 찾을 수 없습니다.");

    private final int httpStatusCode;
    private final String message;
}
