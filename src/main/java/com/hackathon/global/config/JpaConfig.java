package com.hackathon.global.config;

import com.hackathon.global.util.SeoulTime;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing(dateTimeProviderRef = "seoulDateTimeProvider")
public class JpaConfig {

    // 컨테이너 시간대(UTC)와 무관하게 createdAt을 서울 시간으로 남긴다 ("오늘 다녀온 곳" 계산과 맞춘다)
    @Bean
    public DateTimeProvider seoulDateTimeProvider() {
        return () -> Optional.of(LocalDateTime.now(SeoulTime.ZONE));
    }
}
