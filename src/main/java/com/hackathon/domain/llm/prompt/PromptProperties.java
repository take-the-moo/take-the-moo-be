package com.hackathon.domain.llm.prompt;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * prompts.yml의 llm.prompts.* 바인딩. 형식이 틀리면 서버가 시작되지 않는다.
 */
@Validated
@ConfigurationProperties(prefix = "llm")
public record PromptProperties(
        @DefaultValue Map<String, @Valid Spec> prompts
) {

    public record Spec(
            @NotBlank String model,
            @DecimalMin("0.0") @DecimalMax("1.0") Double temperature,
            @Positive Integer maxTokens,
            @NotBlank String system,
            @DefaultValue Map<String, String> defaults
    ) {
    }
}
