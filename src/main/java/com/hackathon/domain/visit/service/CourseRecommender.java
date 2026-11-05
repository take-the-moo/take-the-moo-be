package com.hackathon.domain.visit.service;

import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.service.PlaceService;
import com.hackathon.domain.visit.course.Course;
import com.hackathon.domain.visit.course.CourseRegistry;
import com.hackathon.domain.visit.dto.response.CourseStopDto;
import com.hackathon.domain.visit.dto.response.NextStopDto;
import com.hackathon.domain.visit.dto.response.RecommendedCourseDto;
import com.hackathon.domain.weather.dto.WeatherCondition;
import com.hackathon.domain.weather.service.WeatherService;
import com.hackathon.global.common.Language;
import com.hackathon.global.common.TimeSlot;
import com.hackathon.global.util.GeoUtils;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 도장을 찍은 장소에서 이어 갈 코스 1개와 다음 장소를 고른다. AI를 부르지 않는다 (도장은 바로 응답한다).
 * 코스: 이번 도장으로 완주한 코스 → 미완주 중 많이 진행한 코스 → 남은 거리가 짧은 코스 → 파일 순서
 * 다음 장소: 지금 시간대에 맞고 비·눈이면 실내인 곳 중 가장 가까운 곳. 그런 곳이 없으면 남은 곳 중 가장 가까운 곳.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CourseRecommender {

    private final CourseRegistry courseRegistry;
    private final PlaceService placeService;
    private final WeatherService weatherService;

    /** visitedPlaceIds에는 방금 찍은 장소도 들어 있어야 한다. 이어 갈 코스가 없으면 null */
    public RecommendedCourseDto recommend(Place current, boolean firstVisit, Set<Long> visitedPlaceIds,
                                          Language language, LocalTime time, WeatherCondition simulatedWeather) {
        List<Course> courses = courseRegistry.findByContentId(current.getContentId());
        if (courses.isEmpty()) return null;

        Map<String, Place> places = placeService.getActivePlacesByContentIds(
                        courses.stream().flatMap(course -> course.contentIds().stream()).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Place::getContentId, Function.identity()));

        List<Progress> progresses = new ArrayList<>();
        for (Course course : courses) {
            if (!places.keySet().containsAll(course.contentIds())) {
                // 다 돌 수 없는 코스를 진행 중으로 보여 주지 않는다 (동기화 전이거나 장소가 비활성이 된 경우)
                log.warn("노출 중이 아닌 장소가 있어 추천에서 뺀 코스: courseId={}", course.courseId());
                continue;
            }
            List<Place> stops = course.contentIds().stream().map(places::get).toList();
            progresses.add(Progress.of(course, stops, current, visitedPlaceIds));
        }

        Optional<Progress> justCompleted = firstVisit
                ? progresses.stream().filter(Progress::completed).findFirst()
                : Optional.empty();
        Optional<Progress> chosen = justCompleted.or(() -> progresses.stream()
                .filter(progress -> !progress.completed())
                // 정렬은 안정적이라 앞의 두 조건이 같으면 파일 순서를 유지한다
                .min(Comparator.comparingInt(Progress::visitedCount).reversed()
                        .thenComparingInt(Progress::remainingMeters)));
        if (chosen.isEmpty()) return null;

        Progress progress = chosen.get();
        NextStopDto next = null;
        if (!progress.completed()) {
            WeatherCondition weather = weatherService.resolve(simulatedWeather).condition();
            Place nextPlace = selectNext(current, progress.remaining(), time, weather);
            next = new NextStopDto(nextPlace.getId(), nextPlace.getName(),
                    GeoUtils.walkingMinutes(distance(current, nextPlace)));
        }
        List<CourseStopDto> stops = progress.stops().stream()
                .map(stop -> new CourseStopDto(stop.getId(), stop.getName(), visitedPlaceIds.contains(stop.getId())))
                .toList();
        return new RecommendedCourseDto(progress.course().courseId(), progress.course().localizedName(language),
                progress.visitedCount(), stops.size(), progress.completed(), stops, next);
    }

    /** remaining은 비어 있지 않아야 한다 */
    static Place selectNext(Place current, List<Place> remaining, LocalTime time, WeatherCondition weather) {
        Optional<TimeSlot> slot = TimeSlot.from(time);
        List<Place> suitable = remaining.stream()
                // 밤처럼 맞는 시간대가 없으면 시간대 조건을 뺀다 (AGENTS.md F2 규칙)
                .filter(place -> slot.map(place.getTimeSlots()::contains).orElse(true))
                .filter(place -> !weather.prefersIndoor() || Boolean.TRUE.equals(place.getIndoor()))
                .toList();
        List<Place> candidates = suitable.isEmpty() ? remaining : suitable;
        return candidates.stream()
                .min(Comparator.comparingInt(place -> distance(current, place)))
                .orElseThrow();
    }

    /** 지금 장소에서 남은 곳을 모두 도는 가장 짧은 직선거리 */
    static int shortestRemainingMeters(Place from, List<Place> remaining) {
        int best = remaining.isEmpty() ? 0 : Integer.MAX_VALUE;
        for (Place next : remaining) {
            List<Place> rest = new ArrayList<>(remaining);
            rest.remove(next);
            best = Math.min(best, distance(from, next) + shortestRemainingMeters(next, rest));
        }
        return best;
    }

    private static int distance(Place a, Place b) {
        return GeoUtils.distanceMeters(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
    }

    private record Progress(Course course, List<Place> stops, List<Place> remaining, int remainingMeters) {

        static Progress of(Course course, List<Place> stops, Place current, Set<Long> visitedPlaceIds) {
            List<Place> remaining = stops.stream().filter(stop -> !visitedPlaceIds.contains(stop.getId())).toList();
            return new Progress(course, stops, remaining, shortestRemainingMeters(current, remaining));
        }

        int visitedCount() {
            return stops.size() - remaining.size();
        }

        boolean completed() {
            return remaining.isEmpty();
        }
    }
}
