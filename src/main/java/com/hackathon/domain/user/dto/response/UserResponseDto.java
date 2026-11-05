package com.hackathon.domain.user.dto.response;

import com.hackathon.domain.user.entity.AppUser;
import com.hackathon.global.common.Language;
import com.hackathon.global.common.Nationality;
import io.swagger.v3.oas.annotations.media.Schema;

/** 시각(createdAt)은 내보내지 않는다 (docs/db.md 공통 규칙). sample도 내부 구분용이라 내보내지 않는다 */
@Schema(description = "익명 사용자")
public record UserResponseDto(
        @Schema(description = "서버가 만든 사용자 ID (UUID). 프론트가 기기에 저장한다",
                example = "3f2b8c1e-7a4d-4e9b-9c2a-1d5e6f7a8b9c")
        String userId,

        @Schema(description = "국적", example = "VN")
        Nationality nationality,

        @Schema(description = "이야기·번역에 쓸 언어", example = "VI")
        Language language
) {

    public static UserResponseDto from(AppUser user) {
        return new UserResponseDto(user.getId(), user.getNationality(), user.getLanguage());
    }
}
