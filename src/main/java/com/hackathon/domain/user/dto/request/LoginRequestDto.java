package com.hackathon.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 요청")
public record LoginRequestDto(
        @Schema(description = "로그인 아이디", example = "linh_2026", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "아이디를 입력해 주세요.")
        String loginId,

        @Schema(description = "비밀번호", example = "seodaemun1908", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "비밀번호를 입력해 주세요.")
        String password
) {
}
