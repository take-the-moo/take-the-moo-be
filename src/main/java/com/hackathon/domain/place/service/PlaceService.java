package com.hackathon.domain.place.service;

import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.entity.PlaceStatus;
import com.hackathon.domain.place.exception.PlaceErrorType;
import com.hackathon.domain.place.exception.PlaceException;
import com.hackathon.domain.place.repository.PlaceRepository;
import com.hackathon.global.util.GeoUtils;
import java.util.Comparator;
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

    /** 노출 중(ACTIVE)인 장소 전체. 추천(F2)·주변 장소(F4) 후보는 여기서만 고른다 */
    public List<Place> getActivePlaces() {
        return placeRepository.findAllByStatus(PlaceStatus.ACTIVE);
    }

    /** 노출 중인 장소 전체를 기준 위치에서 가까운 순으로. 거리가 같으면 placeId 순. 거리 제한은 없다 */
    public List<NearbyPlace> getActivePlacesByDistance(BaseLocation location) {
        return getActivePlaces().stream()
                .map(place -> new NearbyPlace(place, GeoUtils.distanceMeters(
                        location.latitude(), location.longitude(), place.getLatitude(), place.getLongitude())))
                .sorted(Comparator.comparingInt(NearbyPlace::distanceMeters)
                        .thenComparing(nearby -> nearby.place().getId()))
                .toList();
    }

    /** F4 주변 장소: 현장 해설 원문(fieldExplanation)이 있는 노출 장소만, 가까운 순으로 limit개 */
    public List<NearbyPlace> getNearbyWithExplanation(BaseLocation location, int limit) {
        return getActivePlacesByDistance(location).stream()
                .filter(nearby -> nearby.place().fieldExplanation() != null)
                .limit(limit)
                .toList();
    }

    /** contentId로 장소를 찾는다. 노출 상태와 상관없이 주고, 없는 장소는 결과에서 빠진다 (F5 코스) */
    public List<Place> getPlacesByContentIds(Collection<String> contentIds) {
        if (contentIds.isEmpty()) return List.of();
        return placeRepository.findAllByContentIdIn(contentIds);
    }

    /** id로 장소를 찾는다. 노출 상태와 상관없이 주고, 없는 장소는 결과에서 빠진다 (F5 방문 기록: 사라진 장소의 기록도 남긴다) */
    public List<Place> getPlacesByIds(Collection<Long> placeIds) {
        if (placeIds.isEmpty()) return List.of();
        return placeRepository.findAllById(placeIds);
    }

    /** TourAPI contentId로 노출 중(ACTIVE)인 장소를 찾는다. 환경마다 placeId가 달라 데이터 파일은 contentId로 장소를 가리킨다 (F6 샘플 한걸음) */
    public Optional<Place> findActiveByContentId(String contentId) {
        return placeRepository.findByContentId(contentId)
                .filter(place -> place.getStatus() == PlaceStatus.ACTIVE);
    }
}
