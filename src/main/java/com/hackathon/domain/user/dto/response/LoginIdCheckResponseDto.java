package com.hackathon.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "아이디 중복 확인 결과")
public record LoginIdCheckResponseDto(
        @Schema(description = "확인한 아이디", example = "linh_2026")
        String loginId,

        @Schema(description = "true면 쓸 수 있다 (아직 아무도 쓰지 않는다)", example = "true")
        boolean available
) {
}
