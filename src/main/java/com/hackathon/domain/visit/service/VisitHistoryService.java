package com.hackathon.domain.visit.service;

import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.service.PlaceService;
import com.hackathon.domain.user.dto.response.UserResponseDto;
import com.hackathon.domain.user.service.UserService;
import com.hackathon.domain.visit.course.CourseRegistry;
import com.hackathon.domain.visit.dto.response.VisitHistoryResponseDto;
import com.hackathon.domain.visit.dto.response.VisitHistoryResponseDto.CourseProgressDto;
import com.hackathon.domain.visit.dto.response.VisitHistoryResponseDto.Day;
import com.hackathon.domain.visit.dto.response.VisitHistoryResponseDto.PlaceSummary;
import com.hackathon.domain.visit.dto.response.VisitHistoryResponseDto.Stamp;
import com.hackathon.domain.visit.entity.Visit;
import com.hackathon.domain.visit.repository.VisitRepository;
import com.hackathon.global.common.Language;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * F5 나의 기록. 도장, 안 간 코스 장소, 코스 진행도, 날짜별 기록을 한 번에 준다.
 * 사라진 장소(비활성)의 기록도 남기고, 장소 자체를 찾을 수 없는 기록만 뺀다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisitHistoryService {

    private final UserService userService;
    private final PlaceService placeService;
    private final CourseRegistry courseRegistry;
    private final CourseProgressReader courseProgressReader;
    private final VisitRepository visitRepository;

    public VisitHistoryResponseDto getHistory(String userId) {
        UserResponseDto user = userService.getUser(userId);
        List<Visit> visits = visitRepository.findAllByUserIdOrderByVisitedDateDescIdAsc(user.userId());
        Set<Long> visitedPlaceIds = visits.stream().map(Visit::getPlaceId).collect(Collectors.toSet());

        Map<Long, Place> places = placeService.getPlacesByIds(visitedPlaceIds).stream()
                .collect(Collectors.toMap(Place::getId, Function.identity()));
        List<Visit> known = visits.stream().filter(visit -> places.containsKey(visit.getPlaceId())).toList();
        if (known.size() != visits.size()) {
            log.warn("장소를 찾을 수 없어 기록에서 뺀 도장: {}건", visits.size() - known.size());
        }

        List<CourseProgress> progresses = courseProgressReader.read(courseRegistry.findAll(), visitedPlaceIds).stream()
                // 안정 정렬이라 진행도가 같으면 파일 순서를 유지한다
                .sorted(Comparator.comparing(CourseProgress::completed).reversed()
                        .thenComparing(Comparator.comparingInt(CourseProgress::visitedCount).reversed()))
                .toList();

        List<Stamp> stamps = stamps(known, places);
        List<CourseProgressDto> courses = progresses.stream()
                .map(progress -> toDto(progress, user.language()))
                .toList();
        int completedCourseCount = (int) progresses.stream().filter(CourseProgress::completed).count();
        return new VisitHistoryResponseDto(stamps.size(), completedCourseCount, stamps,
                undiscovered(progresses), courses, days(known, places));
    }

    /**
     * F8 나의 발자취에 쓸 방문 정보. 도장 수와 완주 코스 수는 나의 기록과 같은 값이다.
     * 완주 날짜는 코스의 장소를 모두 처음 방문한 날이라 다시 방문해도 바뀌지 않는다.
     */
    public JourneyVisits getJourneyVisits(String userId) {
        UserResponseDto user = userService.getUser(userId);
        List<Visit> visits = visitRepository.findAllByUserIdOrderByVisitedDateDescIdAsc(user.userId());
        Set<Long> visitedPlaceIds = visits.stream().map(Visit::getPlaceId).collect(Collectors.toSet());
        Map<Long, Place> places = placeService.getPlacesByIds(visitedPlaceIds).stream()
                .collect(Collectors.toMap(Place::getId, Function.identity()));
        List<Visit> known = visits.stream().filter(visit -> places.containsKey(visit.getPlaceId())).toList();
        List<Stamp> stamps = stamps(known, places);
        List<JourneyVisits.CompletedCourse> completed = courseProgressReader
                .read(courseRegistry.findAll(), visitedPlaceIds).stream()
                .filter(CourseProgress::completed)
                .flatMap(progress -> completedCourse(progress, stamps, user.language()).stream())
                .toList();
        return new JourneyVisits(stamps,
                known.isEmpty() ? null : known.getFirst().getVisitedDate(),
                completed);
    }

    /** 최근 순인 도장 중 이 코스의 장소를 처음 만난 곳이 마지막으로 방문한 곳이다 */
    private static Optional<JourneyVisits.CompletedCourse> completedCourse(CourseProgress progress, List<Stamp> stamps,
                                                                           Language language) {
        Set<Long> stopIds = progress.stops().stream().map(Place::getId).collect(Collectors.toSet());
        return stamps.stream()
                .filter(stamp -> stopIds.contains(stamp.placeId()))
                .findFirst()
                .map(last -> new JourneyVisits.CompletedCourse(progress.course().localizedName(language),
                        last.firstVisitedDate(), last.placeId(), last.name()));
    }

    /** 장소당 하나. 처음 방문한 날이 최근인 순, 같은 날이면 나중에 찍은 도장이 먼저 */
    private static List<Stamp> stamps(List<Visit> visits, Map<Long, Place> places) {
        // visits는 최신 날짜 순이라, 덮어쓰면 장소마다 가장 오래된 방문이 남는다
        Map<Long, Visit> firstVisits = new LinkedHashMap<>();
        visits.forEach(visit -> firstVisits.put(visit.getPlaceId(), visit));
        return firstVisits.values().stream()
                .sorted(Comparator.comparing(Visit::getVisitedDate).thenComparing(Visit::getId).reversed())
                .map(visit -> new Stamp(visit.getPlaceId(), places.get(visit.getPlaceId()).getName(),
                        visit.getVisitedDate()))
                .toList();
    }

    /** 진행 순으로 정렬된 코스의 남은 장소를 차례로 모은다. 그래서 진행 중인 코스의 장소가 먼저 온다 */
    private static List<PlaceSummary> undiscovered(List<CourseProgress> progresses) {
        Map<Long, PlaceSummary> remaining = new LinkedHashMap<>();
        for (CourseProgress progress : progresses) {
            for (Place place : progress.remaining()) {
                remaining.putIfAbsent(place.getId(), new PlaceSummary(place.getId(), place.getName()));
            }
        }
        return List.copyOf(remaining.values());
    }

    private static List<Day> days(List<Visit> visits, Map<Long, Place> places) {
        Map<LocalDate, List<PlaceSummary>> byDate = new LinkedHashMap<>();
        for (Visit visit : visits) {
            Place place = places.get(visit.getPlaceId());
            byDate.computeIfAbsent(visit.getVisitedDate(), date -> new ArrayList<>())
                    .add(new PlaceSummary(place.getId(), place.getName()));
        }
        return byDate.entrySet().stream().map(entry -> new Day(entry.getKey(), entry.getValue())).toList();
    }

    private static CourseProgressDto toDto(CourseProgress progress, Language language) {
        return new CourseProgressDto(progress.course().courseId(), progress.course().localizedName(language),
                progress.visitedCount(), progress.stops().size(), progress.completed(), progress.stopDtos());
    }
}
