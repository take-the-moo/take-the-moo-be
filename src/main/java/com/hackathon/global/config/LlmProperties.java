package com.hackathon.global.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * application.yml의 llm.* 설정. 프롬프트(llm.prompts)는 PromptProperties가 따로 바인딩한다.
 */
@Validated
@ConfigurationProperties(prefix = "llm")
public record LlmProperties(
        @DefaultValue("false") boolean mock,
        @NotNull @DefaultValue Timeout timeout,
        @DefaultValue("1") int maxRetries
) {

    public record Timeout(
            @DefaultValue("5s") Duration connect,
            @DefaultValue("60s") Duration read,
            @DefaultValue("120s") Duration request
    ) {
    }
}
