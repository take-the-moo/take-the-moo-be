package com.hackathon.global.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 지금 요청의 로그인 상태. AuthTokenFilter가 Authorization 헤더를 읽어 요청에 담고, UserService가 읽는다.
 * 요청 밖(비동기 엽서 생성 등 서버 내부 작업)에서는 NO_REQUEST다.
 */
public record AuthContext(State state, String userId) {

    static final String ATTRIBUTE = AuthContext.class.getName();

    public enum State {
        /** 유효한 토큰. userId가 있다 */
        AUTHENTICATED,
        /** Authorization 헤더가 없다 */
        ANONYMOUS,
        /** 헤더는 있지만 토큰이 위조·만료·형식 오류다 */
        INVALID,
        /** HTTP 요청 밖에서 부른 서버 내부 작업 */
        NO_REQUEST
    }

    static final AuthContext ANONYMOUS = new AuthContext(State.ANONYMOUS, null);
    static final AuthContext INVALID = new AuthContext(State.INVALID, null);
    static final AuthContext NO_REQUEST = new AuthContext(State.NO_REQUEST, null);

    static AuthContext authenticated(String userId) {
        return new AuthContext(State.AUTHENTICATED, userId);
    }

    /** 지금 요청의 로그인 상태 */
    public static AuthContext current() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servlet)) return NO_REQUEST;
        HttpServletRequest request = servlet.getRequest();
        Object value = request.getAttribute(ATTRIBUTE);
        return value instanceof AuthContext context ? context : ANONYMOUS;
    }
}
