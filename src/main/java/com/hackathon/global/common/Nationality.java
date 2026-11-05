package com.hackathon.global.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 국적. API에서는 상수 이름(VN, CN …)으로 주고받는다.
 * koreanName은 프롬프트 변수({nationality})에, defaultLanguage는 처음 설정 화면의 기본 선택에 쓴다.
 * 시연 국적은 베트남 + 1개국. 국적을 늘리면 Language도 함께 확인한다.
 */
@Getter
@RequiredArgsConstructor
public enum Nationality {

    VN("베트남", Language.VI),
    CN("중국", Language.ZH),
    JP("일본", Language.JA),
    MN("몽골", Language.MN),
    UZ("우즈베키스탄", Language.UZ),
    US("미국", Language.EN),
    OTHER("기타", Language.EN);

    private final String koreanName;
    private final Language defaultLanguage;
}
