package com.hackathon.domain.place.service;

import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.entity.PlaceStatus;
import com.hackathon.domain.place.exception.PlaceErrorType;
import com.hackathon.domain.place.exception.PlaceException;
import com.hackathon.domain.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 다른 도메인이 장소를 조회하는 진입점 (repository를 직접 쓰지 않는다). 필요한 메서드만 추가한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService {

    private final PlaceRepository placeRepository;

    /** 노출 중(ACTIVE)인 장소. 없거나 노출하지 않는 장소면 404 PLACE_NOT_FOUND */
    public Place getActivePlace(Long placeId) {
        return placeRepository.findById(placeId)
                .filter(place -> place.getStatus() == PlaceStatus.ACTIVE)
                .orElseThrow(() -> new PlaceException(PlaceErrorType.NOT_FOUND, "placeId=" + placeId));
    }
}
