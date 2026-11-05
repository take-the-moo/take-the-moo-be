package com.hackathon.domain.user.exception;

import com.hackathon.global.exception.DomainException;

public class UserException extends DomainException {

    public UserException(UserErrorType errorType) {
        super(errorType);
    }

    /** detail은 로그에만 남는다. */
    public UserException(UserErrorType errorType, String detail) {
        super(errorType, detail);
    }
}
