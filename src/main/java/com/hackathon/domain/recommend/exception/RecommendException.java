package com.hackathon.domain.recommend.exception;

import com.hackathon.global.exception.DomainException;

public class RecommendException extends DomainException {

    public RecommendException(RecommendErrorType errorType) {
        super(errorType);
    }

    /** detail은 로그에만 남는다. 위치(lat/lng) 값은 넣지 않는다. */
    public RecommendException(RecommendErrorType errorType, String detail) {
        super(errorType, detail);
    }
}
