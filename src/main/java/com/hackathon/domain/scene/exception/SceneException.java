package com.hackathon.domain.scene.exception;

import com.hackathon.global.exception.DomainException;

public class SceneException extends DomainException {

    public SceneException(SceneErrorType errorType) {
        super(errorType);
    }

    /** detail은 로그에만 남는다. */
    public SceneException(SceneErrorType errorType, String detail) {
        super(errorType, detail);
    }
}
