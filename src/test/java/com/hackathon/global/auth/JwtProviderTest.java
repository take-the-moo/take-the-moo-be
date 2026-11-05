package com.hackathon.global.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hackathon.global.config.AppProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

    private static final String SECRET = "test-only-jwt-secret-0123456789abcdef";

    private static JwtProvider provider(String secret, Duration ttl) {
        return new JwtProvider(new AppProperties(null, null, null, null, null, null,
                new AppProperties.Auth(secret, ttl, true)));
    }

    @Test
    void 발급한_토큰에서_userId를_꺼낸다() {
        JwtProvider provider = provider(SECRET, Duration.ofDays(30));
        JwtProvider.IssuedToken token = provider.issue("linh_2026");

        assertThat(provider.verify(token.token())).contains("linh_2026");
        assertThat(token.expiresAt()).isAfter(java.time.Instant.now().plus(Duration.ofDays(29)));
    }

    @Test
    void 만료된_토큰은_받지_않는다() {
        JwtProvider provider = provider(SECRET, Duration.ofSeconds(-60));

        assertThat(provider.verify(provider.issue("linh_2026").token())).isEmpty();
    }

    @Test
    void 다른_키로_서명한_토큰은_받지_않는다() {
        String token = provider("another-secret-0123456789abcdefghij", Duration.ofDays(1)).issue("linh_2026").token();

        assertThat(provider(SECRET, Duration.ofDays(1)).verify(token)).isEmpty();
        assertThat(provider(SECRET, Duration.ofDays(1)).verify("not-a-jwt")).isEmpty();
    }

    @Test
    void 서명_키가_짧으면_서버가_뜨지_않는다() {
        assertThatThrownBy(() -> provider("too-short", Duration.ofDays(1)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 서명_키가_없으면_임시_키로_동작한다() {
        JwtProvider provider = provider("", Duration.ofDays(1));

        assertThat(provider.verify(provider.issue("linh_2026").token())).contains("linh_2026");
    }
}
