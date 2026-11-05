package com.hackathon.domain.llm.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "서버 상태")
public record HealthResponseDto(
        @Schema(description = "상태", example = "ok") String status,
        @Schema(description = "목 모드 여부", example = "true") boolean mock
) {
}
