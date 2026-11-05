package com.hackathon.domain.llm.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

@Schema(description = "구조화 분석 요청")
public record AnalyzeRequestDto(
        @Schema(description = "prompts.yml의 키. 비우면 analyze", example = "analyze", defaultValue = "analyze")
        @Size(max = 50, message = "50자 이하로 입력해 주세요.")
        String promptKey,

        @Schema(description = "분석할 입력 텍스트", example = "오늘 회의: 프론트는 로그인 화면 완료, 백엔드는 LLM 연동 중. 내일 오전까지 데모 시나리오 확정 필요.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "분석할 내용을 입력해 주세요.")
        @Size(max = 20_000, message = "20000자 이하로 입력해 주세요.")
        String input,

        @Schema(description = "시스템 프롬프트의 {변수} 치환 값", example = "{\"domain\": \"프로젝트 관리\"}")
        @Size(max = 20, message = "20개 이하로 보내 주세요.")
        Map<String, String> params
) {

    public String promptKeyOrDefault() {
        return promptKey == null || promptKey.isBlank() ? "analyze" : promptKey;
    }
}
