package com.hackathon.domain.user.dto.request;

import com.hackathon.global.common.Language;
import com.hackathon.global.common.Nationality;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 요청")
public record SignUpRequestDto(
        @Schema(description = "로그인 아이디. 영문 소문자·숫자·밑줄(_) 4~20자. 가입 뒤 바꿀 수 없다", example = "linh_2026",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "아이디를 입력해 주세요.")
        @Pattern(regexp = LoginIdRule.REGEX, message = LoginIdRule.MESSAGE)
        String loginId,

        @Schema(description = "비밀번호 8~64자", example = "seodaemun1908", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Size(min = 8, max = 64, message = "비밀번호는 8~64자로 입력해 주세요.")
        String password,

        @Schema(description = "화면에 보이는 이름 1~20자", example = "린", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "이름을 입력해 주세요.")
        @Size(max = 20, message = "이름은 20자 이하로 입력해 주세요.")
        String name,

        @Schema(description = "국적", example = "VN", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "국적을 선택해 주세요.")
        Nationality nationality,

        @Schema(description = "이야기·번역에 쓸 언어", example = "VI", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "언어를 선택해 주세요.")
        Language language
) {
}
