package com.hackathon.domain.visit.exception;

import com.hackathon.global.exception.DomainException;

public class VisitException extends DomainException {

    public VisitException(VisitErrorType errorType) {
        super(errorType);
    }

    /** detail은 로그에만 남는다. 사용자 위치(좌표·거리)를 넣지 않는다. */
    public VisitException(VisitErrorType errorType, String detail) {
        super(errorType, detail);
    }
}
