package com.hackathon.domain.user.dto.request;

import com.hackathon.global.common.Language;
import com.hackathon.global.common.Nationality;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "처음 설정 — 익명 사용자 생성 요청")
public record UserCreateRequestDto(
        @Schema(description = "국적", example = "VN", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "국적을 선택해 주세요.")
        Nationality nationality,

        @Schema(description = "이야기·번역에 쓸 언어", example = "VI", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "언어를 선택해 주세요.")
        Language language
) {
}
