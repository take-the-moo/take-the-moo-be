package com.hackathon.global.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authorization: Bearer 토큰을 읽어 요청에 로그인 상태(AuthContext)를 담는다.
 * 여기서는 막지 않는다. 사용자가 필요한 API에서 UserService가 상태를 보고 401·403을 던진다
 * (필터에서 던지면 공통 오류 응답 형식을 거치지 않는다). 토큰이 필요 없는 API(추천·지도 등)는 그대로 지나간다.
 */
@Component
@RequiredArgsConstructor
public class AuthTokenFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        request.setAttribute(AuthContext.ATTRIBUTE, resolve(request.getHeader(HttpHeaders.AUTHORIZATION)));
        chain.doFilter(request, response);
    }

    private AuthContext resolve(String header) {
        if (header == null || header.isBlank()) return AuthContext.ANONYMOUS;
        if (!header.regionMatches(true, 0, BEARER, 0, BEARER.length())) return AuthContext.INVALID;
        return jwtProvider.verify(header.substring(BEARER.length()).strip())
                .map(AuthContext::authenticated)
                .orElse(AuthContext.INVALID);
    }
}
