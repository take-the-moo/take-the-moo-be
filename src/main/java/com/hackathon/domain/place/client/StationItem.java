package com.hackathon.domain.place.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** T Data 지하철역 좌표정보 한 행. convX = 경도, convY = 위도 (문자열) */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StationItem(
        String outStnNum,
        String stnKrNm,
        String lineNm,
        String convX,
        String convY
) {

    public boolean isValid() {
        return outStnNum != null && !outStnNum.isBlank()
                && stnKrNm != null && !stnKrNm.isBlank()
                && positive(convX) && positive(convY);
    }

    private static boolean positive(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            return Double.parseDouble(value) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
