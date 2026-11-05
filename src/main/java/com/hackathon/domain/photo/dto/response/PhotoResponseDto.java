package com.hackathon.domain.photo.dto.response;

import com.hackathon.domain.photo.entity.Photo;
import io.swagger.v3.oas.annotations.media.Schema;

/** 올린 시각과 작성자 ID는 내보내지 않는다 */
@Schema(description = "업로드한 사진")
public record PhotoResponseDto(
        @Schema(description = "사진 ID (UUID). 한걸음·엽서에 이 값을 넘긴다", example = "9b1c2d3e-4f5a-6b7c-8d9e-0f1a2b3c4d5e")
        String photoId,

        @Schema(description = "사진 파일 주소 (서버 기준 경로). <img src>에 그대로 쓴다",
                example = "/api/photos/9b1c2d3e-4f5a-6b7c-8d9e-0f1a2b3c4d5e")
        String url,

        @Schema(description = "찍은 장소 ID. 없이 올렸으면 null", example = "7", nullable = true)
        Long placeId
) {

    public static PhotoResponseDto from(Photo photo) {
        return new PhotoResponseDto(photo.getId(), urlOf(photo.getId()), photo.getPlaceId());
    }

    public static String urlOf(String photoId) {
        return "/api/photos/" + photoId;
    }
}
