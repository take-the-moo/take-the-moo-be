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
 * - 팀 보강: data/place-curation.json 값, 없으면 타입별 기본값. 동기화 때마다 파일 기준으로 다시 계산한다
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
    /** 추천 방문 시간대. "MORNING,AFTERNOON" 형태 (MORNING 06–11 · AFTERNOON 11–17 · EVENING 17–21) */
    @Column(length = 100)
    private String recommendedTimeSlots;

    /** 실내 여부. 비 오면 실내만 추천한다 */
    private Boolean indoor;

    /** 식민지 역사가 얽힌 장소. 프롬프트에 "기억과 평화의 공간" 원칙을 건다 */
    private boolean sensitiveHistory;

    /** 현장 해설 원문 (한국어). AI는 이걸 번역만 한다. 보강 파일에 없으면 null */
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

    static final String CULTURAL_FACILITY = "14";
    static final List<String> DAYTIME = List.of("MORNING", "AFTERNOON");
    static final List<String> ALL_DAY = List.of("MORNING", "AFTERNOON", "EVENING");

    /**
     * 노출 여부와 보강 필드를 정한다. 동기화 때마다 호출된다.
     * 1. excluded → EXCLUDED
     * 2. 보강 파일에 있거나 자동 노출 타입 → ACTIVE (보강 값이 없으면 기본값)
     * 3. 그 외 → PENDING_REVIEW
     * 기본값: 실내 = 문화시설이면 true / 시간대 = 문화시설은 낮(MORNING, AFTERNOON), 그 외는 하루 종일
     */
    public void applyRules(PlaceCuration curation, boolean autoActiveType, boolean excluded) {
        boolean culturalFacility = CULTURAL_FACILITY.equals(contentTypeId);
        List<String> slots = curation != null && curation.timeSlots() != null && !curation.timeSlots().isEmpty()
                ? curation.timeSlots()
                : culturalFacility ? DAYTIME : ALL_DAY;

        this.recommendedTimeSlots = String.join(",", slots);
        this.indoor = curation != null && curation.indoor() != null ? curation.indoor() : culturalFacility;
        this.sensitiveHistory = curation != null && Boolean.TRUE.equals(curation.sensitiveHistory());
        this.explanation = curation == null ? null : curation.explanation();

        if (excluded) this.status = PlaceStatus.EXCLUDED;
        else if (curation != null || autoActiveType) this.status = PlaceStatus.ACTIVE;
        else this.status = PlaceStatus.PENDING_REVIEW;
    }

    public void deactivate() {
        this.status = PlaceStatus.INACTIVE;
    }
}
