package com.hackathon.domain.place.service;

import com.hackathon.domain.place.client.TourApiClient;
import com.hackathon.domain.place.client.TourPlaceItem;
import com.hackathon.domain.place.curation.PlaceCurationRegistry;
import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.entity.PlaceStatus;
import com.hackathon.domain.place.repository.PlaceRepository;
import com.hackathon.global.config.AppProperties;
import com.hackathon.global.external.ExternalApiException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * TourAPI → place 테이블 동기화.
 * 1. 관광 타입별로 서대문구 목록을 전부 받는다 (하나라도 실패하면 DB를 건드리지 않고 중단)
 * 2. contentId로 upsert. TourAPI 필드만 갱신하고, modifiedtime이 바뀐 장소만 개요를 다시 받는다
 * 3. 팀 보강 파일에 있으면 ACTIVE, 없으면 PENDING_REVIEW
 * 4. 이번 목록에 없는 기존 장소는 INACTIVE (삭제하지 않는다)
 * 외부 호출이 길어 트랜잭션은 걸지 않는다 (장소마다 save가 각각 커밋된다).
 */
@Slf4j
@Service
public class PlaceSyncService {

    private final TourApiClient client;
    private final PlaceRepository placeRepository;
    private final PlaceCurationRegistry curationRegistry;
    private final List<String> contentTypeIds;

    public PlaceSyncService(TourApiClient client, PlaceRepository placeRepository,
                            PlaceCurationRegistry curationRegistry, AppProperties properties) {
        this.client = client;
        this.placeRepository = placeRepository;
        this.curationRegistry = curationRegistry;
        this.contentTypeIds = properties.tour().contentTypeIds();
    }

    public boolean isConfigured() {
        return client.isConfigured();
    }

    /** @return 이번에 받은 장소 수 */
    public int sync() {
        Map<String, TourPlaceItem> fetched = new LinkedHashMap<>();
        for (String contentTypeId : contentTypeIds) {
            for (TourPlaceItem item : client.fetchAreaPlaces(contentTypeId)) {
                if (item.contentid() != null && item.hasCoordinates()) fetched.put(item.contentid(), item);
            }
        }

        int overviews = 0;
        for (TourPlaceItem item : fetched.values()) {
            Place place = placeRepository.findByContentId(item.contentid())
                    .orElseGet(() -> Place.fromTour(item.contentid()));
            boolean needsOverview = place.needsOverview(item.modifiedtime());
            place.updateTourInfo(item.contenttypeid(), item.title(), item.fullAddress(),
                    Double.parseDouble(item.mapy()), Double.parseDouble(item.mapx()),
                    blankToNull(item.firstimage()), item.modifiedtime());
            if (needsOverview) {
                try {
                    client.fetchOverview(item.contentid()).ifPresent(place::updateOverview);
                    overviews++;
                } catch (ExternalApiException e) {
                    // 개요 하나 실패로 전체를 멈추지 않는다. 다음 동기화 때 다시 받는다
                    log.warn("장소 개요 조회 실패: contentId={}, {}", item.contentid(), e.getDetail());
                }
            }
            place.applyCuration(curationRegistry.find(item.contentid()).orElse(null));
            placeRepository.save(place);
        }

        int deactivated = deactivateMissing(fetched.keySet());
        log.info("장소 동기화 완료: 받은 {}건, 개요 갱신 {}건, 비활성 {}건, 보강 데이터 {}건",
                fetched.size(), overviews, deactivated, curationRegistry.size());
        return fetched.size();
    }

    private int deactivateMissing(Set<String> fetchedContentIds) {
        Set<String> seen = new HashSet<>(fetchedContentIds);
        int count = 0;
        for (Place place : placeRepository.findAll()) {
            if (place.getStatus() != PlaceStatus.INACTIVE && !seen.contains(place.getContentId())) {
                place.deactivate();
                placeRepository.save(place);
                count++;
            }
        }
        return count;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
