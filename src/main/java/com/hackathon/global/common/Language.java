package com.hackathon.global.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 표시 언어. API에서는 상수 이름(VI, EN …)으로 주고받는다.
 * koreanName은 프롬프트 변수({language})에 넣는 이름이다.
 */
@Getter
@RequiredArgsConstructor
public enum Language {

    KO("한국어"),
    EN("영어"),
    VI("베트남어"),
    ZH("중국어"),
    JA("일본어"),
    MN("몽골어"),
    UZ("우즈베크어");

    private final String koreanName;
}
