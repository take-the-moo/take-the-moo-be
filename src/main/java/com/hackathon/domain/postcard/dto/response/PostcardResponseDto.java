package com.hackathon.domain.postcard.dto.response;

import com.hackathon.domain.postcard.entity.Postcard;
import com.hackathon.domain.postcard.entity.PostcardStatus;
import com.hackathon.domain.postcard.entity.PostcardTemplate;
import io.swagger.v3.oas.annotations.media.Schema;

/** 작성자 ID와 시각은 내보내지 않는다 */
@Schema(description = "엽서 생성 상태")
public record PostcardResponseDto(
        @Schema(description = "엽서 ID (UUID)", example = "c7d8e9f0-1a2b-3c4d-5e6f-7a8b9c0d1e2f")
        String postcardId,

        @Schema(description = "PENDING(대기) → PROCESSING(만드는 중) → COMPLETED(완성) / FAILED(실패)", example = "COMPLETED")
        PostcardStatus status,

        @Schema(description = "템플릿", example = "BASIC")
        PostcardTemplate template,

        @Schema(description = "엽서 이미지 주소 (서버 기준 경로). COMPLETED일 때만, 아니면 null",
                example = "/api/postcards/c7d8e9f0-1a2b-3c4d-5e6f-7a8b9c0d1e2f/image", nullable = true)
        String imageUrl,

        @Schema(description = "내려받기 주소 (파일로 저장). COMPLETED일 때만, 아니면 null",
                example = "/api/postcards/c7d8e9f0-1a2b-3c4d-5e6f-7a8b9c0d1e2f/image?download=true", nullable = true)
        String downloadUrl,

        @Schema(description = "실패 사유. FAILED일 때만 (PHOTO_FILE_MISSING, RENDER_FAILED, INTERRUPTED 등)",
                example = "null", nullable = true)
        String errorCode
) {

    public static PostcardResponseDto from(Postcard postcard) {
        boolean completed = postcard.getStatus() == PostcardStatus.COMPLETED;
        String imageUrl = "/api/postcards/" + postcard.getId() + "/image";
        return new PostcardResponseDto(postcard.getId(), postcard.getStatus(), postcard.getTemplate(),
                completed ? imageUrl : null, completed ? imageUrl + "?download=true" : null,
                postcard.getStatus() == PostcardStatus.FAILED ? postcard.getErrorCode() : null);
    }
}
