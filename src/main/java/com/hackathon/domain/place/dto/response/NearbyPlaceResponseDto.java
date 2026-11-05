package com.hackathon.domain.place.dto.response;

import com.hackathon.domain.place.service.NearbyPlace;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주변 장소 (F4 현장 해설 후보)")
public record NearbyPlaceResponseDto(
        @Schema(description = "장소 ID. POST /api/explanations의 placeId로 보낸다", example = "34")
        Long placeId,

        @Schema(description = "장소 이름 (한국어)", example = "서대문형무소역사관")
        String name,

        @Schema(description = "대표 이미지 주소. 없으면 null", example = "https://tong.visitkorea.or.kr/cms/resource/example/prison.jpg",
                nullable = true)
        String imageUrl,

        @Schema(description = "기준 위치에서의 직선거리 (미터)", example = "197")
        int distanceMeters
) {

    public static NearbyPlaceResponseDto from(NearbyPlace nearby) {
        return new NearbyPlaceResponseDto(nearby.place().getId(), nearby.place().getName(),
                nearby.place().getImageUrl(), nearby.distanceMeters());
    }
}
