package com.hackathon.domain.place.service;

import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.entity.PlaceStatus;
import com.hackathon.domain.place.exception.PlaceErrorType;
import com.hackathon.domain.place.exception.PlaceException;
import com.hackathon.domain.place.repository.PlaceRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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

    /** contentId로 노출 중(ACTIVE)인 장소를 찾는다. 없거나 노출하지 않는 장소는 결과에서 빠진다 (F5 코스) */
    public List<Place> getActivePlacesByContentIds(Collection<String> contentIds) {
        if (contentIds.isEmpty()) return List.of();
        return placeRepository.findAllByContentIdInAndStatus(contentIds, PlaceStatus.ACTIVE);
    }

    /** TourAPI contentId로 노출 중(ACTIVE)인 장소를 찾는다. 환경마다 placeId가 달라 데이터 파일은 contentId로 장소를 가리킨다 (F6 샘플 한걸음) */
    public Optional<Place> findActiveByContentId(String contentId) {
        return placeRepository.findByContentId(contentId)
                .filter(place -> place.getStatus() == PlaceStatus.ACTIVE);
    }
}
