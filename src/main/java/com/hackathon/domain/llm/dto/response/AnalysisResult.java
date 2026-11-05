package com.hackathon.domain.llm.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * /api/analyze 구조화 출력 샘플 DTO. 당일 주제에 맞게 필드만 바꾸면 된다.
 * 필드가 곧 LLM에 전달되는 JSON 스키마이므로 @Schema description을 구체적으로 쓴다.
 * mock()은 목 모드 응답이다 — 필드를 바꾸면 같이 바꾼다.
 */
@Schema(description = "분석 결과")
public record AnalysisResult(
        @Schema(description = "입력 내용 2~3문장 요약", example = "프론트는 로그인 화면을 마쳤고 백엔드는 LLM 연동을 진행 중이다.")
        String summary,

        @Schema(description = "핵심 포인트 3~5개", example = "[\"로그인 화면 완료\", \"LLM 연동 진행 중\"]")
        List<String> keyPoints,

        @Schema(description = "바로 실행할 액션 아이템 1~5개", example = "[\"내일 오전까지 데모 시나리오 확정\"]")
        List<String> actionItems
) {

    public static AnalysisResult mock() {
        return new AnalysisResult(
                "[MOCK] 입력 내용을 요약한 고정 응답입니다.",
                List.of("[MOCK] 핵심 포인트 1", "[MOCK] 핵심 포인트 2", "[MOCK] 핵심 포인트 3"),
                List.of("[MOCK] 액션 아이템 1", "[MOCK] 액션 아이템 2")
        );
    }
}
