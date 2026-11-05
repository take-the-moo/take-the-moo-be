package com.hackathon.domain.visit.service;

import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.entity.PlaceStatus;
import com.hackathon.domain.place.service.PlaceService;
import com.hackathon.domain.visit.course.Course;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 코스별 진행도를 계산한다. 도장 응답(추천 코스)과 방문 기록이 함께 쓴다.
 * 앞으로 갈 수 없는 장소가 남은 코스는 뺀다. 이미 다녀온 장소는 나중에 비활성이 되어도 진행도에 남긴다
 * (TourAPI에서 장소가 빠졌다고 사용자의 완주 기록이 사라지면 안 된다).
 */
@Slf4j
@Component
@RequiredArgsConstructor
class CourseProgressReader {

    private final PlaceService placeService;

    /** 넘겨준 순서를 유지한다 */
    List<CourseProgress> read(List<Course> courses, Set<Long> visitedPlaceIds) {
        Map<String, Place> places = placeService.getPlacesByContentIds(
                        courses.stream().flatMap(course -> course.contentIds().stream()).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Place::getContentId, Function.identity()));

        List<CourseProgress> progresses = new ArrayList<>();
        for (Course course : courses) {
            if (!isOpen(course, places, visitedPlaceIds)) {
                log.warn("갈 수 없는 장소가 남아 뺀 코스: courseId={}", course.courseId());
                continue;
            }
            List<Place> stops = course.contentIds().stream().map(places::get).toList();
            progresses.add(new CourseProgress(course, stops, visitedPlaceIds));
        }
        return progresses;
    }

    private static boolean isOpen(Course course, Map<String, Place> places, Set<Long> visitedPlaceIds) {
        return course.contentIds().stream().allMatch(contentId -> {
            Place place = places.get(contentId);
            return place != null
                    && (place.getStatus() == PlaceStatus.ACTIVE || visitedPlaceIds.contains(place.getId()));
        });
    }
}
