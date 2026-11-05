package com.hackathon.domain.place.entity;

import com.hackathon.domain.place.curation.PlaceCuration;
import com.hackathon.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 장소. 필드 출처가 둘이다.
 * - TourAPI: 동기화 때마다 갱신 (contentId로 찾는다)
 * - 팀 보강: data/place-curation.json에서만 바뀐다. 동기화가 덮어쓰지 않는다
 * 다른 도메인은 이 엔티티를 연관관계로 묶지 말고 placeId(Long)로 참조한다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── TourAPI ──────────────────────────────
    @Column(nullable = false, unique = true, length = 20)
    private String contentId;

    /** 12 관광지, 14 문화시설, 38 쇼핑 … */
    @Column(length = 10)
    private String contentTypeId;

    @Column(nullable = false)
    private String name;

    private String address;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Lob
    private String overview;

    @Column(length = 500)
    private String imageUrl;

    /** TourAPI modifiedtime (yyyyMMddHHmmss). 바뀌었을 때만 개요를 다시 받는다 */
    @Column(length = 14)
    private String tourModifiedTime;

    // ── 팀 보강 ──────────────────────────────
    /** 추천 방문 시간대. "MORNING,AFTERNOON" 형태 (값 정의는 추천 기능에서) */
    @Column(length = 100)
    private String recommendedTimeSlots;

    /** 실내 여부. 비 오면 실내만 추천한다. 보강 전에는 null */
    private Boolean indoor;

    /** 식민지 역사가 얽힌 장소. 프롬프트에 "기억과 평화의 공간" 원칙을 건다 */
    private boolean sensitiveHistory;

    /** 현장 해설 원문 (한국어). AI는 이걸 번역만 한다 */
    @Lob
    private String explanation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlaceStatus status;

    public static Place fromTour(String contentId) {
        Place place = new Place();
        place.contentId = contentId;
        place.status = PlaceStatus.PENDING_REVIEW;
        return place;
    }

    public void updateTourInfo(String contentTypeId, String name, String address,
                               double latitude, double longitude, String imageUrl, String modifiedTime) {
        this.contentTypeId = contentTypeId;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageUrl = imageUrl;
        this.tourModifiedTime = modifiedTime;
    }

    public void updateOverview(String overview) {
        this.overview = overview;
    }

    public boolean needsOverview(String modifiedTime) {
        return overview == null || tourModifiedTime == null || !tourModifiedTime.equals(modifiedTime);
    }

    /** curation이 있으면 보강 필드를 채우고 ACTIVE, 없으면 PENDING_REVIEW (보강 필드는 그대로 둔다) */
    public void applyCuration(PlaceCuration curation) {
        if (curation == null) {
            this.status = PlaceStatus.PENDING_REVIEW;
            return;
        }
        List<String> slots = curation.timeSlots() == null ? List.of() : curation.timeSlots();
        this.recommendedTimeSlots = String.join(",", slots);
        this.indoor = curation.indoor();
        this.sensitiveHistory = Boolean.TRUE.equals(curation.sensitiveHistory());
        this.explanation = curation.explanation();
        this.status = PlaceStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = PlaceStatus.INACTIVE;
    }
}
