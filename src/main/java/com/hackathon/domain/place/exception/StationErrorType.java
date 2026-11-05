package com.hackathon.domain.place.exception;

import com.hackathon.global.response.type.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StationErrorType implements ErrorCode {

    NOT_FOUND(404, "역을 찾을 수 없습니다."),
    INVALID_LOCATION(422, "위치 값이 올바르지 않습니다. 위도(lat)와 경도(lng)를 함께 보내 주세요.");

    private final int httpStatusCode;
    private final String message;
}
