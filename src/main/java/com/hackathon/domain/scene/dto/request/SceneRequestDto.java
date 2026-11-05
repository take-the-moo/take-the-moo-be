package com.hackathon.domain.scene.dto.request;

import com.hackathon.global.common.Language;
import com.hackathon.global.common.Nationality;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "맞춤 장면 요청. 국적만 바꿔 다시 보내면 같은 장소의 다른 이야기가 온다")
public record SceneRequestDto(
        @Schema(description = "장소 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "장소를 선택해 주세요.")
        @Positive(message = "올바른 장소 ID가 아닙니다.")
        Long placeId,

        @Schema(description = "관점 국적. 보통 사용자 국적, 국적 전환 시연에서는 다른 값", example = "VN",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "국적을 선택해 주세요.")
        Nationality nationality,

        @Schema(description = "이야기 언어 (storyNative·connectionLine)", example = "VI",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "언어를 선택해 주세요.")
        Language language
) {
}
