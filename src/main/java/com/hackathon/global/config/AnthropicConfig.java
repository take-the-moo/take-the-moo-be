package com.hackathon.global.config;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.Timeout;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.AnthropicSetup;
import org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatProperties;
import org.springframework.ai.model.anthropic.autoconfigure.AnthropicConnectionProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Spring AI 기본 설정(spring.ai.anthropic.timeout)은 시간 제한을 하나만 받아 연결/읽기를 나눌 수 없다.
 * 그래서 AnthropicChatModel을 직접 만들어 SDK 클라이언트에 connect/read/request 제한을 따로 건다.
 * 이 빈이 있으면 Spring AI의 자동 설정 빈(@ConditionalOnMissingBean)은 만들어지지 않는다.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class AnthropicConfig {

    // 키가 없을 때 시작은 되게 하는 자리표시자. 실제 호출은 401 → LLM_CALL_FAILED가 된다
    private static final String MISSING_API_KEY = "missing-api-key";

    @Bean
    public AnthropicChatModel anthropicChatModel(
            AnthropicConnectionProperties connectionProperties,
            AnthropicChatProperties chatProperties,
            LlmProperties llmProperties
    ) {
        String apiKey = connectionProperties.getApiKey();
        if (!StringUtils.hasText(apiKey)) {
            if (!llmProperties.mock()) {
                log.warn("ANTHROPIC_API_KEY가 설정되지 않았습니다. 실제 호출은 실패합니다. 목 모드: LLM_MOCK=true");
            }
            apiKey = MISSING_API_KEY;
        }

        LlmProperties.Timeout limits = llmProperties.timeout();
        Timeout timeout = Timeout.builder()
                .connect(limits.connect())
                .read(limits.read())
                .write(limits.read())
                .request(limits.request())
                .build();

        AnthropicClient client = AnthropicSetup
                .setupSyncClient(connectionProperties.getBaseUrl(), apiKey, limits.request(),
                        llmProperties.maxRetries(), null, connectionProperties.getCustomHeaders())
                .withOptions(options -> options.timeout(timeout));

        AnthropicChatOptions defaultOptions = chatProperties.toOptions().mutate()
                .apiKey(apiKey)
                .build();

        // 비동기 클라이언트를 주지 않으면 스트리밍도 위 동기 클라이언트(같은 시간 제한)로 처리된다
        return AnthropicChatModel.builder()
                .anthropicClient(client)
                .options(defaultOptions)
                .build();
    }
}
