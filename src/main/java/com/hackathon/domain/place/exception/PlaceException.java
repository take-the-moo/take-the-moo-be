package com.hackathon.domain.place.exception;

import com.hackathon.global.exception.DomainException;

public class PlaceException extends DomainException {

    public PlaceException(PlaceErrorType errorType) {
        super(errorType);
    }

    /** detail은 로그에만 남는다. */
    public PlaceException(PlaceErrorType errorType, String detail) {
        super(errorType, detail);
    }
}
