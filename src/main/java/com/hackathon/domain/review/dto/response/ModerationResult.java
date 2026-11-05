package com.hackathon.domain.review.dto.response;

import com.hackathon.global.common.Language;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * moderate 프롬프트의 구조화 출력. 필드 설명이 LLM에 전달되는 JSON 스키마가 된다.
 */
@Schema(description = "한걸음 검사 결과")
public record ModerationResult(
        @Schema(description = "그대로 공개해도 되면 true. 욕설·혐오·광고·연락처·스팸이 있으면 false")
        boolean allowed,

        @Schema(description = "걸린 이유. OK, PROFANITY(욕설·성적 표현), HATE(국가·민족·집단 비하), AD(광고·홍보), PERSONAL_INFO(연락처·개인정보), SPAM(의미 없는 반복) 중 하나")
        String reason,

        @Schema(description = "글이 실제로 쓰인 언어. 여러 언어가 섞였으면 가장 많이 쓴 언어")
        Language language
) {

    /** 목 모드: 규칙 필터만 동작하고 AI 검사는 통과로 본다 */
    public static ModerationResult mock(Language hint) {
        return new ModerationResult(true, "OK", hint == null ? Language.KO : hint);
    }
}
