package com.hackathon.domain.place.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** TourAPI detailCommon2 응답 item (개요만 쓴다) */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TourDetailItem(String contentid, String overview) {
}
