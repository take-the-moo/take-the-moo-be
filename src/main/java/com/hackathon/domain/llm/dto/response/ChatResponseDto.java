package com.hackathon.domain.llm.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "텍스트 응답 (/api/chat, /api/image 공통)")
public record ChatResponseDto(
        @Schema(description = "AI 답변", example = "짧은 휴식을 자주 넣고, 물을 충분히 마시세요.") String answer,
        @Schema(description = "응답한 모델 ID", example = "claude-haiku-5-5") String model,
        @Schema(description = "토큰 사용량") UsageDto usage,
        @Schema(description = "LLM 호출 지연 시간(ms)", example = "1834") long latencyMs
) {
}
