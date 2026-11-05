package com.hackathon.domain.postcard.dto.request;

import com.hackathon.domain.postcard.entity.Postcard;
import com.hackathon.domain.postcard.entity.PostcardTemplate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "엽서 생성 요청")
public record PostcardCreateRequestDto(
        @Schema(description = "POST /api/users가 돌려준 userId", example = "3f2b8c1e-7a4d-4e9b-9c2a-1d5e6f7a8b9c",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "사용자 ID가 없습니다. 처음 설정을 다시 해 주세요.")
        @Size(max = 36, message = "올바른 사용자 ID가 아닙니다.")
        String userId,

        @Schema(description = "엽서에 넣을 사진. POST /api/photos가 돌려준 photoId (내 사진만)",
                example = "9b1c2d3e-4f5a-6b7c-8d9e-0f1a2b3c4d5e", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "사진을 골라 주세요.")
        @Size(max = 36, message = "올바른 사진 ID가 아닙니다.")
        String photoId,

        @Schema(description = "엽서에 쓸 한 문장 (100자 이하)", example = "독립문 앞에서 처음으로 한국의 가을을 느꼈다",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "한 문장을 써 주세요.")
        @Size(max = Postcard.COMMENT_MAX, message = "100자 이하로 써 주세요.")
        String comment,

        @Schema(description = "템플릿. 비우면 BASIC", example = "BASIC")
        PostcardTemplate template
) {
}
