package com.hackathon.domain.recommend.exception;

import com.hackathon.global.response.type.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RecommendErrorType implements ErrorCode {

    NOT_FOUND(404, "지금 추천할 장소가 없습니다.");

    private final int httpStatusCode;
    private final String message;
}
