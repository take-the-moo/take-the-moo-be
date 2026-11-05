package com.hackathon.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "회원가입·로그인 결과")
public record AuthResponseDto(
        @Schema(description = "인증 토큰 (JWT). 기기에 저장하고 요청마다 Authorization: Bearer {accessToken}으로 보낸다",
                example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJsaW5oXzIwMjYiLCJpYXQiOjE3OTk5OTk5OTksImV4cCI6MTgwMjU5MTk5OX0.signature")
        String accessToken,

        @Schema(description = "토큰 만료 시각 (UTC, ISO-8601). 지나면 다시 로그인한다", example = "2026-12-07T06:00:00Z")
        Instant expiresAt,

        @Schema(description = "로그인한 사용자")
        UserResponseDto user
) {
}
