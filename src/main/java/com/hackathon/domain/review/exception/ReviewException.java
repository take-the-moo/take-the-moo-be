package com.hackathon.domain.review.exception;

import com.hackathon.global.exception.DomainException;

public class ReviewException extends DomainException {

    public ReviewException(ReviewErrorType errorType) {
        super(errorType);
    }

    /** detail은 로그에만 남는다. 후기 원문은 넣지 않는다 */
    public ReviewException(ReviewErrorType errorType, String detail) {
        super(errorType, detail);
    }
}
