package com.hackathon.domain.journey.service;

import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.review.service.UserReview;
import com.hackathon.domain.visit.service.JourneyVisits;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 나의 발자취 화면과 AI 이야기가 함께 쓰는 사용자의 기록.
 *
 * @param reviews 찾을 수 있는 장소의 한걸음. 오래된 순
 * @param places  도장과 한걸음에 나온 장소
 */
record JourneyRecords(JourneyVisits visits, List<UserReview> reviews, Map<Long, Place> places) {

    boolean isEmpty() {
        return visits.stamps().isEmpty() && reviews.isEmpty();
    }

    /** 도장과 한걸음 중 가장 이른 날짜. 기록이 없으면 null */
    LocalDate startDate() {
        Stream<LocalDate> stampDates = visits.stamps().stream().map(stamp -> stamp.firstVisitedDate());
        Stream<LocalDate> reviewDates = reviews.stream().map(UserReview::date);
        return Stream.concat(stampDates, reviewDates).min(LocalDate::compareTo).orElse(null);
    }

    /** 도장과 한걸음 중 가장 늦은 날짜. 기록이 없으면 null */
    LocalDate endDate() {
        Stream<LocalDate> visitDate = Stream.ofNullable(visits.lastVisitDate());
        Stream<LocalDate> reviewDates = reviews.stream().map(UserReview::date);
        return Stream.concat(visitDate, reviewDates).max(LocalDate::compareTo).orElse(null);
    }
}
