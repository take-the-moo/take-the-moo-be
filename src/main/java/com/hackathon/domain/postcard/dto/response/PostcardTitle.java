package com.hackathon.domain.postcard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** postcard-title 프롬프트의 구조화 출력: 엽서 제목 한 줄 (사용자 언어로 날짜·장소) */
public record PostcardTitle(
        @Schema(description = "엽서 제목 한 줄. 예: 2026년 11월 7일, 독립문에서")
        String title
) {
}
