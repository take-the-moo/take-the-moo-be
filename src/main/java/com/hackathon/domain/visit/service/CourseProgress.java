package com.hackathon.domain.visit.service;

import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.visit.course.Course;
import com.hackathon.domain.visit.dto.response.CourseStopDto;
import java.util.List;
import java.util.Set;

/** 사용자 한 명의 코스 진행도. 날짜와 순서에 상관없이 다녀온 곳을 센다 */
record CourseProgress(Course course, List<Place> stops, Set<Long> visitedPlaceIds) {

    List<Place> remaining() {
        return stops.stream().filter(stop -> !visitedPlaceIds.contains(stop.getId())).toList();
    }

    int visitedCount() {
        return stops.size() - remaining().size();
    }

    boolean completed() {
        return remaining().isEmpty();
    }

    List<CourseStopDto> stopDtos() {
        return stops.stream()
                .map(stop -> new CourseStopDto(stop.getId(), stop.getName(), visitedPlaceIds.contains(stop.getId())))
                .toList();
    }
}
