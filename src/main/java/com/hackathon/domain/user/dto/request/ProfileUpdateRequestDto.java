package com.hackathon.domain.user.dto.request;

import com.hackathon.global.common.Language;
import com.hackathon.global.common.Nationality;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "내 정보 수정. 바꿀 항목만 보낸다 (보내지 않은 항목은 그대로)")
public record ProfileUpdateRequestDto(
        @Schema(description = "이름 1~20자", example = "린")
        @Size(max = 20, message = "이름은 20자 이하로 입력해 주세요.")
        @Pattern(regexp = ".*\\S.*", message = "이름을 입력해 주세요.")
        String name,

        @Schema(description = "국적", example = "VN")
        Nationality nationality,

        @Schema(description = "이야기·번역에 쓸 언어", example = "EN")
        Language language
) {
}
