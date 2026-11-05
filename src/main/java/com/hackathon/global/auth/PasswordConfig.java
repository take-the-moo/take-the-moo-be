package com.hackathon.global.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** 비밀번호 해시 (BCrypt). spring-security-crypto 모듈만 쓴다 — Spring Security의 필터·로그인 화면은 없다 */
@Configuration(proxyBeanMethods = false)
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
