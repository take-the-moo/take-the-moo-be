package com.hackathon.global.storage;

import com.hackathon.global.exception.DomainException;

public class StorageException extends DomainException {

    public StorageException(StorageErrorType errorType, String detail) {
        super(errorType, detail);
    }

    public StorageException(StorageErrorType errorType, String detail, Throwable cause) {
        super(errorType, detail);
        initCause(cause);
    }
}
