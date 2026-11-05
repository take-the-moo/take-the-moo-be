package com.hackathon.domain.journey.sample;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** data/demo-journey.json. 형식은 파일 안 _guide에 적혀 있다 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DemoJourneyFile(List<Visit> visits, List<Review> reviews) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Visit(String contentId, Integer daysAgo) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Review(String contentId, Integer daysAgo, String content) {
    }
}
