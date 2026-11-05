package com.hackathon.domain.visit.sample;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hackathon.global.common.Nationality;
import java.util.List;
import java.util.Map;

/** data/sample-visits.json. 형식은 파일 안 _guide에 적혀 있다 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SampleVisitFile(List<Entry> places) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entry(
            String contentId,
            String name,
            Map<Nationality, Integer> visitors
    ) {
    }
}
