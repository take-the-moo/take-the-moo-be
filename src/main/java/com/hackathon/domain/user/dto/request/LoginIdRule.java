package com.hackathon.domain.user.dto.request;

/** 로그인 아이디 규칙. 가입과 중복 확인이 같이 쓴다 */
public final class LoginIdRule {

    /** 영문 소문자·숫자·밑줄 4~20자. 샘플 사용자 id(UUID)는 '-'가 있어 이 규칙에 맞지 않는다 */
    public static final String REGEX = "^[a-z0-9_]{4,20}$";
    public static final String MESSAGE = "아이디는 영문 소문자·숫자·밑줄(_) 4~20자로 입력해 주세요.";

    private LoginIdRule() {}
}
