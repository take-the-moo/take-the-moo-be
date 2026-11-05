package com.hackathon.domain.user.dto.response;

import com.hackathon.domain.user.entity.AppUser;
import com.hackathon.global.common.Language;
import com.hackathon.global.common.Nationality;
import io.swagger.v3.oas.annotations.media.Schema;

/** 시각(createdAt)은 내보내지 않는다 (docs/db.md 공통 규칙). 비밀번호·sample도 내보내지 않는다 */
@Schema(description = "사용자")
public record UserResponseDto(
        @Schema(description = "사용자 ID = 로그인 아이디. 다른 API의 userId에 쓴다", example = "linh_2026")
        String userId,

        @Schema(description = "이름", example = "린")
        String name,

        @Schema(description = "국적", example = "VN")
        Nationality nationality,

        @Schema(description = "이야기·번역에 쓸 언어", example = "VI")
        Language language
) {

    public static UserResponseDto from(AppUser user) {
        return new UserResponseDto(user.getId(), user.getName(), user.getNationality(), user.getLanguage());
    }
}
