package com.hackathon.domain.place.service;

import com.hackathon.domain.place.client.TourText;
import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.exception.PlaceErrorType;
import com.hackathon.domain.place.exception.PlaceException;
import com.hackathon.global.external.ExternalHttp;
import com.hackathon.global.external.ExternalImage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 장소 사진을 서버를 거쳐 준다 (F8). 관광공사 사진은 CORS 헤더가 없어 웹에서 화면을 캡처하면 저장이 실패할 수 있다.
 * 요청에서 주소를 받지 않고 DB에 저장된 그 장소의 주소만 부른다. 노출하지 않게 된 장소도 사용자의 기록에는 남으므로 함께 준다.
 */
@Service
@RequiredArgsConstructor
public class PlaceImageService {

    private static final int MAX_BYTES = 10 * 1024 * 1024;

    private final PlaceService placeService;
    private final ExternalHttp externalHttp;

    public ExternalImage getImage(Long placeId) {
        List<Place> places = placeService.getPlacesByIds(List.of(placeId));
        if (places.isEmpty()) {
            throw new PlaceException(PlaceErrorType.NOT_FOUND, "placeId=" + placeId);
        }
        String url = TourText.toHttps(places.getFirst().getImageUrl());
        if (url == null) {
            throw new PlaceException(PlaceErrorType.IMAGE_NOT_FOUND, "placeId=" + placeId);
        }
        return externalHttp.getImage("장소 사진", url, MAX_BYTES);
    }

    /** 응답에 넣을 장소 사진 주소. 사진이 없는 장소면 null */
    public static String urlOf(Place place) {
        String url = place.getImageUrl();
        return url == null || url.isBlank() ? null : "/api/places/" + place.getId() + "/image";
    }
}
