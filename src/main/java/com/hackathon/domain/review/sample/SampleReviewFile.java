package com.hackathon.domain.review.sample;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hackathon.global.common.Language;
import com.hackathon.global.common.Nationality;
import java.util.List;

/** data/sample-reviews.json. 형식은 파일 안 _guide에 적혀 있다 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SampleReviewFile(List<Entry> reviews) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entry(
            String contentId,
            String name,
            Nationality nationality,
            Language language,
            String content,
            Integer daysAgo
    ) {

        boolean isFilled() {
            return content != null && !content.isBlank();
        }
    }
}
