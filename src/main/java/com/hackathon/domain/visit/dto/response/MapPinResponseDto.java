package com.hackathon.domain.visit.dto.response;

import com.hackathon.global.common.Nationality;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 흔적 지도의 핀 하나 = 장소 하나 (F6). 장소 단위 집계만 담는다.
 * 개인의 위치·방문 날짜·시각, 작성자 ID는 내보내지 않는다 (좌표는 장소의 좌표다).
 */
@Schema(description = "흔적 지도 핀 (장소별 집계)")
public record MapPinResponseDto(
        @Schema(description = "장소 ID. GET /api/places/{placeId}/reviews로 한걸음을 본다", example = "4")
        Long placeId,

        @Schema(description = "장소 이름 (한국어)", example = "독립문")
        String name,

        @Schema(description = "장소 위도", example = "37.5723")
        double latitude,

        @Schema(description = "장소 경도", example = "126.9598")
        double longitude,

        @Schema(description = "대표 이미지 주소. 없으면 null", nullable = true,
                example = "https://tong.visitkorea.or.kr/cms/resource/example/gate.jpg")
        String imageUrl,

        @Schema(description = "다녀간 사람 수 (방문 도장 기준, 한 사람이 여러 번 와도 1명)", example = "7")
        long visitorCount,

        @Schema(description = "한걸음 수", example = "4")
        long reviewCount,

        @Schema(description = "다녀간 사람의 국적 분포. 많은 순 (같으면 국적 코드 순서). 방문자가 없으면 빈 배열")
        List<NationalityCount> nationalities
) {

    @Schema(description = "국적별 방문자 수")
    public record NationalityCount(
            @Schema(description = "국적", example = "VN")
            Nationality nationality,

            @Schema(description = "그 국적의 방문자 수", example = "3")
            long count
    ) {
    }
}
