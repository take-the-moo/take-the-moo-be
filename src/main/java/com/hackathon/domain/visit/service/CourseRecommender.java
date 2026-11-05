package com.hackathon.domain.visit.service;

import com.hackathon.domain.place.entity.Place;
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
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 도장을 찍은 장소에서 이어 갈 코스 1개와 다음 장소를 고른다. AI를 부르지 않는다 (도장은 바로 응답한다).
 * 코스: 이번 도장으로 완주한 코스 → 미완주 중 많이 진행한 코스 → 남은 거리가 짧은 코스 → 파일 순서
 * 다음 장소: 지금 시간대에 맞고 비·눈이면 실내인 곳 중 가장 가까운 곳. 그런 곳이 없으면 남은 곳 중 가장 가까운 곳.
 */
@Component
@RequiredArgsConstructor
public class CourseRecommender {

    private final CourseRegistry courseRegistry;
    private final CourseProgressReader courseProgressReader;
    private final WeatherService weatherService;

    /** visitedPlaceIds에는 방금 찍은 장소도 들어 있어야 한다. 이어 갈 코스가 없으면 null */
    public RecommendedCourseDto recommend(Place current, boolean firstVisit, Set<Long> visitedPlaceIds,
                                          Language language, LocalTime time, WeatherCondition simulatedWeather) {
        List<Course> courses = courseRegistry.findByContentId(current.getContentId());
        if (courses.isEmpty()) return null;

        List<CourseProgress> progresses = courseProgressReader.read(courses, visitedPlaceIds);
        Optional<CourseProgress> justCompleted = firstVisit
                ? progresses.stream().filter(CourseProgress::completed).findFirst()
                : Optional.empty();
        Optional<CourseProgress> chosen = justCompleted.or(() -> progresses.stream()
                .filter(progress -> !progress.completed())
                // min은 같은 값이면 앞의 것을 고르므로, 앞의 두 조건이 같으면 파일 순서를 유지한다
                .min(Comparator.comparingInt(CourseProgress::visitedCount).reversed()
                        .thenComparingInt(progress -> shortestRemainingMeters(current, progress.remaining()))));
        if (chosen.isEmpty()) return null;

        CourseProgress progress = chosen.get();
        NextStopDto next = null;
        if (!progress.completed()) {
            WeatherCondition weather = weatherService.resolve(simulatedWeather).condition();
            Place nextPlace = selectNext(current, progress.remaining(), time, weather);
            next = new NextStopDto(nextPlace.getId(), nextPlace.getName(),
                    GeoUtils.walkingMinutes(distance(current, nextPlace)));
        }
        List<CourseStopDto> stops = progress.stopDtos();
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
}
