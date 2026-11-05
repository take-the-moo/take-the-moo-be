package com.hackathon.global.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** 공공데이터포털 응답 response.header. 성공 코드는 서비스마다 다르다 (TourAPI "0000", 기상청 "00") */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PublicDataHeader(String resultCode, String resultMsg) {
}
