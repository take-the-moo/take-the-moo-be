package com.hackathon.domain.review.dto.response;

import com.hackathon.domain.review.entity.Review;
import com.hackathon.global.common.Language;
import com.hackathon.global.common.Nationality;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/** 작성자 ID와 작성 시각은 내보내지 않는다. 날짜까지만 (docs/db.md 공통 규칙) */
@Schema(description = "한걸음")
public record ReviewResponseDto(
        @Schema(description = "한걸음 ID", example = "12")
        Long reviewId,

        @Schema(description = "장소 ID", example = "7")
        Long placeId,

        @Schema(description = "원문", example = "독립문 앞에서 사진 찍기 좋아요. 아침에 오면 한적해요.")
        String content,

        @Schema(description = "원문 언어", example = "KO")
        Language language,

        @Schema(description = "작성자 국적 (국기 표시용)", example = "VN")
        Nationality nationality,

        @Schema(description = "작성 날짜 (서울)", example = "2026-11-07")
        LocalDate writtenDate
) {

    public static ReviewResponseDto of(Review review, Nationality nationality) {
        return new ReviewResponseDto(review.getId(), review.getPlaceId(), review.getContent(), review.getLanguage(),
                nationality, review.getCreatedAt().toLocalDate());
    }
}
