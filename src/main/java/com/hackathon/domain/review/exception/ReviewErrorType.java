package com.hackathon.domain.review.exception;

import com.hackathon.global.response.type.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReviewErrorType implements ErrorCode {

    /** 욕설·혐오·광고·연락처가 들어 있다. 저장하지 않는다 */
    INAPPROPRIATE(422, "한걸음에 남길 수 없는 내용이 있어요. 욕설·광고·연락처 없이 다시 써 주세요.");

    private final int httpStatusCode;
    private final String message;
}
