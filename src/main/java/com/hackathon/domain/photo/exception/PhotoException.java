package com.hackathon.domain.photo.exception;

import com.hackathon.global.exception.DomainException;

public class PhotoException extends DomainException {

    public PhotoException(PhotoErrorType errorType, String detail) {
        super(errorType, detail);
    }
}
