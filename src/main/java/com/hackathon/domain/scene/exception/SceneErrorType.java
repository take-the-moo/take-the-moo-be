package com.hackathon.domain.scene.exception;

import com.hackathon.global.response.type.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SceneErrorType implements ErrorCode {

    /** 장소에 해설·개요가 없다. AI가 지어내지 않도록 이야기를 만들지 않는다 */
    SOURCE_MISSING(422, "이 장소는 아직 이야기를 만들 자료가 없습니다.");

    private final int httpStatusCode;
    private final String message;
}
