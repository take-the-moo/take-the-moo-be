package com.hackathon.domain.place.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** TourAPI areaBasedList2 응답 item. mapx = 경도, mapy = 위도 (문자열) */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TourPlaceItem(
        String contentid,
        String contenttypeid,
        String title,
        String addr1,
        String addr2,
        String mapx,
        String mapy,
        String firstimage,
        String modifiedtime
) {

    public boolean hasCoordinates() {
        return isNumber(mapx) && isNumber(mapy);
    }

    public String fullAddress() {
        if (addr2 == null || addr2.isBlank()) return addr1;
        return addr1 + " " + addr2;
    }

    private static boolean isNumber(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            return Double.parseDouble(value) != 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
