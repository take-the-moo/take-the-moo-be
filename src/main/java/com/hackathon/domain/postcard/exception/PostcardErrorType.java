package com.hackathon.domain.postcard.exception;

import com.hackathon.global.response.type.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PostcardErrorType implements ErrorCode {

    NOT_FOUND(404, "엽서를 찾을 수 없습니다."),
    NOT_READY(409, "엽서가 아직 완성되지 않았습니다. 상태를 다시 확인해 주세요.");

    private final int httpStatusCode;
    private final String message;
}
