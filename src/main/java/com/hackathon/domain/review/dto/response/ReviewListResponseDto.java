package com.hackathon.domain.review.dto.response;

import com.hackathon.domain.review.entity.Review;
import com.hackathon.global.common.Language;
import com.hackathon.global.common.Nationality;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "장소의 한걸음 목록 (최신순)")
public record ReviewListResponseDto(
        @Schema(description = "장소 ID", example = "7")
        Long placeId,

        @Schema(description = "번역에 쓴 언어 (보는 사람 언어)", example = "VI")
        Language viewerLanguage,

        List<Item> reviews
) {

    /** 작성자 ID와 작성 시각은 내보내지 않는다. 날짜까지만 (docs/db.md 공통 규칙) */
    @Schema(description = "한걸음 하나")
    public record Item(
            @Schema(description = "한걸음 ID", example = "12")
            Long reviewId,

            @Schema(description = "원문", example = "아침에 오면 한적해서 사진 찍기 좋아요")
            String content,

            @Schema(description = "원문 언어", example = "KO")
            Language language,

            @Schema(description = "보는 사람 언어로 번역한 글. 원문 언어와 같거나 번역에 실패하면 null — 원문을 보여 준다",
                    example = "Buổi sáng vắng người nên chụp ảnh rất đẹp")
            String translatedContent,

            @Schema(description = "작성자 국적 (국기 표시용)", example = "JP")
            Nationality nationality,

            @Schema(description = "작성 날짜 (서울)", example = "2026-11-07")
            LocalDate writtenDate,

            @Schema(description = "true면 시연용 샘플 한걸음 (실제 사용자가 쓴 글이 아니다)", example = "false")
            boolean sample
    ) {

        public static Item of(Review review, String translatedContent, Nationality nationality, boolean sample) {
            return new Item(review.getId(), review.getContent(), review.getLanguage(), translatedContent, nationality,
                    review.getCreatedAt().toLocalDate(), sample);
        }
    }
}
