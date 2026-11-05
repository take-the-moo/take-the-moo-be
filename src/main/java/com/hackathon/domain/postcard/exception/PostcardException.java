package com.hackathon.domain.postcard.exception;

import com.hackathon.global.exception.DomainException;

public class PostcardException extends DomainException {

    public PostcardException(PostcardErrorType errorType, String detail) {
        super(errorType, detail);
    }
}
