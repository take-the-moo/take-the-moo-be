package com.hackathon.global.external;

import com.hackathon.global.exception.DomainException;

/**
 * 공공데이터 등 외부 API 호출 실패. detail에는 API 이름과 원인 종류만 넣는다.
 * 요청 URL에는 인증키가 들어 있으므로 원본 예외(cause)를 붙이거나 URL·예외 메시지를 detail에 넣지 않는다.
 */
public class ExternalApiException extends DomainException {

    public ExternalApiException(ExternalApiErrorType errorType, String detail) {
        super(errorType, detail);
    }
}
