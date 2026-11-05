package com.hackathon.domain.llm.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

@Schema(description = "텍스트 채팅 요청")
public record ChatRequestDto(
        @Schema(description = "prompts.yml의 키. 비우면 chat", example = "chat", defaultValue = "chat")
        @Size(max = 50, message = "50자 이하로 입력해 주세요.")
        String promptKey,

        @Schema(description = "사용자 메시지", example = "해커톤에서 밤샘할 때 집중력 유지하는 팁 알려줘",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "메시지를 입력해 주세요.")
        @Size(max = 10_000, message = "10000자 이하로 입력해 주세요.")
        String message,

        @Schema(description = "시스템 프롬프트의 {변수} 치환 값", example = "{\"target\": \"해커톤 참가자\"}")
        @Size(max = 20, message = "20개 이하로 보내 주세요.")
        Map<String, String> params
) {

    public String promptKeyOrDefault() {
        return promptKey == null || promptKey.isBlank() ? "chat" : promptKey;
    }
}
