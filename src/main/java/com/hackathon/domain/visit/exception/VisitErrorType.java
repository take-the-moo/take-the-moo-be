package com.hackathon.domain.visit.exception;

import com.hackathon.global.response.type.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VisitErrorType implements ErrorCode {

    /** 장소에서 도장 반경(app.visit.stamp-radius-meters) 밖이다 */
    TOO_FAR(422, "장소 근처에서만 도장을 받을 수 있습니다.");

    private final int httpStatusCode;
    private final String message;
}
