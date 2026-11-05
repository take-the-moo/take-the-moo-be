package com.hackathon.domain.llm.exception;

import com.hackathon.global.exception.DomainException;

public class LlmException extends DomainException {

    public LlmException(LlmErrorType errorType) {
        super(errorType);
    }

    /** detail은 로그에만 남는다. 모델 원본 오류·promptKey 등 디버그 정보를 넣는다. */
    public LlmException(LlmErrorType errorType, String detail) {
        super(errorType, detail);
    }

    public LlmException(LlmErrorType errorType, String detail, Throwable cause) {
        super(errorType, detail);
        initCause(cause);
    }
}
