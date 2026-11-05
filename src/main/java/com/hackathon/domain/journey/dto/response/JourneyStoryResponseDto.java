package com.hackathon.domain.journey.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "나의 발자취 이야기")
public record JourneyStoryResponseDto(
        @Schema(description = "이야기. 기록이 없거나, 저장된 글이 없는데 AI 호출이 실패하면 null")
        JourneyStoryText story
) {
}
