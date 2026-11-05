package com.hackathon.global.auth;

import com.hackathon.global.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 앱 내 로그인 토큰 (JWT, HS256). 토큰에는 sub(= 로그인 아이디 = userId)와 발급·만료 시각만 넣는다.
 * 토큰은 DB에 저장하지 않고 서명과 만료로만 확인한다. 토큰 원문은 로그에 남기지 않는다.
 */
@Slf4j
@Component
public class JwtProvider {

    static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final Duration ttl;

    public JwtProvider(AppProperties properties) {
        this.key = Keys.hmacShaKeyFor(secretBytes(properties.auth().jwtSecret()));
        this.ttl = properties.auth().tokenTtl();
    }

    /** 로그인·가입 때 발급한다 */
    public IssuedToken issue(String userId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);
        String token = Jwts.builder()
                .subject(userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        return new IssuedToken(token, expiresAt);
    }

    /** 서명·만료가 맞으면 userId. 위조·만료·형식 오류면 empty */
    public Optional<String> verify(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            String subject = claims.getSubject();
            return subject == null || subject.isBlank() ? Optional.empty() : Optional.of(subject);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static byte[] secretBytes(String secret) {
        if (secret == null || secret.isBlank()) {
            log.warn("app.auth.jwt-secret이 없어 임시 서명 키를 만든다. 서버를 다시 켜면 모든 로그인이 풀린다 (배포 환경에서는 꼭 넣는다)");
            byte[] random = new byte[MIN_SECRET_BYTES];
            new SecureRandom().nextBytes(random);
            return random;
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("app.auth.jwt-secret은 " + MIN_SECRET_BYTES + "바이트 이상이어야 한다");
        }
        return bytes;
    }

    /** 발급한 토큰과 만료 시각 */
    public record IssuedToken(String token, Instant expiresAt) {
    }
}
