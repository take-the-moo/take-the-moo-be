package com.hackathon.domain.visit.service;

import com.hackathon.domain.visit.dto.response.VisitHistoryResponseDto.Stamp;
import java.time.LocalDate;
import java.util.List;

/**
 * F8 나의 발자취에 넘기는 방문 정보.
 *
 * @param stamps        도장. 처음 방문한 날이 최근인 순 (나의 기록과 같다)
 * @param lastVisitDate 마지막으로 방문한 날. 도장이 없으면 null
 * @param completed     완주한 코스
 */
public record JourneyVisits(List<Stamp> stamps, LocalDate lastVisitDate, List<CompletedCourse> completed) {

    /**
     * @param name      사용자 언어의 코스 이름
     * @param date      코스의 장소를 모두 방문한 날
     * @param placeId   마지막으로 방문한 장소
     * @param placeName 그 장소 이름
     */
    public record CompletedCourse(String name, LocalDate date, Long placeId, String placeName) {
    }
}
