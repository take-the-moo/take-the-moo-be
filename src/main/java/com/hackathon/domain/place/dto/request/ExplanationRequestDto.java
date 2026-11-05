package com.hackathon.domain.place.dto.request;

import com.hackathon.global.common.Language;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "현장 해설 요청")
public record ExplanationRequestDto(
        @Schema(description = "장소 ID (GET /api/places/nearby의 placeId)", example = "34",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "장소를 선택해 주세요.")
        @Positive(message = "올바른 장소 ID가 아닙니다.")
        Long placeId,

        @Schema(description = "읽을 언어. KO면 원문을 그대로 준다", example = "VI", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "언어를 선택해 주세요.")
        Language language
) {
}
