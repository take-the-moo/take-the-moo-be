package com.hackathon.domain.llm.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "토큰 사용량")
public record UsageDto(
        @Schema(description = "입력 토큰 수", example = "152") int inputTokens,
        @Schema(description = "출력 토큰 수", example = "348") int outputTokens
) {

    public static final UsageDto EMPTY = new UsageDto(0, 0);
}
