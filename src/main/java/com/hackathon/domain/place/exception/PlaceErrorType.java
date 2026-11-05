package com.hackathon.domain.place.exception;

import com.hackathon.global.response.type.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlaceErrorType implements ErrorCode {

    NOT_FOUND(404, "장소를 찾을 수 없습니다.");

    private final int httpStatusCode;
    private final String message;
}
