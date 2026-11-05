package com.hackathon.domain.review.dto.request;

import com.hackathon.domain.review.entity.Review;
import com.hackathon.global.common.Language;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "한걸음 작성 요청")
public record ReviewCreateRequestDto(
        @Schema(description = "POST /api/users가 돌려준 userId", example = "3f2b8c1e-7a4d-4e9b-9c2a-1d5e6f7a8b9c",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "사용자 ID가 없습니다. 처음 설정을 다시 해 주세요.")
        @Size(max = 36, message = "올바른 사용자 ID가 아닙니다.")
        String userId,

        @Schema(description = "한 줄 후기 (200자 이하)", example = "독립문 앞에서 사진 찍기 좋아요. 아침에 오면 한적해요.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "한걸음을 입력해 주세요.")
        @Size(max = Review.CONTENT_MAX, message = "200자 이하로 써 주세요.")
        String content,

        @Schema(description = "실제로 쓴 언어. 비우면 서버가 글을 보고 판단한다 (사용자 설정 언어를 그대로 넣지 않는다)",
                example = "KO")
        Language language,

        @Schema(description = "첨부 사진 (선택). POST /api/photos가 돌려준 photoId — 내가 올린 사진만 붙일 수 있다",
                example = "9b1c2d3e-4f5a-6b7c-8d9e-0f1a2b3c4d5e")
        @Size(max = 36, message = "올바른 사진 ID가 아닙니다.")
        String photoId
) {

    /** 사진 없이 쓰는 한걸음 */
    public ReviewCreateRequestDto(String userId, String content, Language language) {
        this(userId, content, language, null);
    }
}
