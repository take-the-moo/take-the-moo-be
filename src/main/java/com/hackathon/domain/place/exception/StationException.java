package com.hackathon.domain.place.exception;

import com.hackathon.global.exception.DomainException;

public class StationException extends DomainException {

    public StationException(StationErrorType errorType) {
        super(errorType);
    }

    /** detail은 로그에만 남는다. 위치(lat/lng) 값은 넣지 않는다. */
    public StationException(StationErrorType errorType, String detail) {
        super(errorType, detail);
    }
}
