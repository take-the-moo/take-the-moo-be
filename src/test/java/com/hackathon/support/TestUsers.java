package com.hackathon.support;

import com.hackathon.domain.user.service.UserService;
import com.hackathon.global.common.Language;
import com.hackathon.global.common.Nationality;
import java.util.UUID;

/**
 * 테스트용 가입 사용자. 아이디는 매번 새로 만든다 (H2를 테스트끼리 같이 쓴다).
 * 테스트 설정은 토큰 검사를 끄므로(app.auth.enforce-token=false) 돌려받은 userId로 바로 API를 부를 수 있다.
 */
public final class TestUsers {

    /** 테스트 전용 비밀번호 */
    public static final String PASSWORD = "test-password-1234";

    private TestUsers() {}

    /** 가입하고 userId(= 로그인 아이디)를 돌려준다 */
    public static String create(UserService userService, Nationality nationality, Language language) {
        String loginId = newLoginId();
        return userService.signUp(loginId, PASSWORD, "테스트", nationality, language).userId();
    }

    /** 규칙(영문 소문자·숫자·밑줄 4~20자)에 맞는 새 아이디 */
    public static String newLoginId() {
        return "t_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
