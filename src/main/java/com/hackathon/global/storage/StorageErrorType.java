package com.hackathon.global.storage;

import com.hackathon.global.response.type.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StorageErrorType implements ErrorCode {

    NOT_FOUND(404, "파일을 찾을 수 없습니다."),
    WRITE_FAILED(500, "파일을 저장하지 못했습니다.");

    private final int httpStatusCode;
    private final String message;
}
