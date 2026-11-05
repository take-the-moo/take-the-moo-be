package com.hackathon.domain.visit.service;

import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.service.PlaceService;
import com.hackathon.domain.user.dto.response.UserResponseDto;
import com.hackathon.domain.user.service.UserService;
import com.hackathon.domain.visit.dto.request.VisitRequestDto;
import com.hackathon.domain.visit.dto.response.PlaceVisitorCount;
import com.hackathon.domain.visit.dto.response.RecommendedCourseDto;
import com.hackathon.domain.visit.dto.response.VisitResponseDto;
import com.hackathon.domain.visit.entity.Visit;
import com.hackathon.domain.visit.exception.VisitErrorType;
import com.hackathon.domain.visit.exception.VisitException;
import com.hackathon.domain.visit.repository.VisitRepository;
import com.hackathon.global.config.AppProperties;
import com.hackathon.global.util.GeoUtils;
import com.hackathon.global.util.SeoulTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * F5 방문 도장. 장소 근처에서만 찍히고, 하루에 한 장소당 하나다.
 * 요청 좌표는 거리 계산에만 쓰고 저장·로그하지 않는다 (예외 detail에도 넣지 않는다).
 */
@Service
public class VisitService {

    private final UserService userService;
    private final PlaceService placeService;
    private final CourseRecommender courseRecommender;
    private final VisitRepository visitRepository;
    private final int stampRadiusMeters;

    public VisitService(UserService userService, PlaceService placeService, CourseRecommender courseRecommender,
                        VisitRepository visitRepository, AppProperties properties) {
        this.userService = userService;
        this.placeService = placeService;
        this.courseRecommender = courseRecommender;
        this.visitRepository = visitRepository;
        this.stampRadiusMeters = properties.visit().stampRadiusMeters();
    }

    /** 시연 계정의 도장이 이미 들어갔는지 본다 */
    public boolean hasVisits(String userId) {
        return visitRepository.countByUserId(userId) > 0;
    }

    /** 시연 계정의 도장을 지난 날짜로 넣는다. 위치를 확인하지 않으므로 실제 사용자에게는 쓰지 않는다 */
    public void addDemoVisit(String userId, Long placeId, LocalDate visitedDate) {
        visitRepository.save(Visit.create(userId, placeId, visitedDate));
    }

    public VisitResponseDto stamp(VisitRequestDto request) {
        UserResponseDto user = userService.getUser(request.userId());
        Place place = placeService.getActivePlace(request.placeId());
        LocalDate today = SeoulTime.today();

        Optional<Visit> todayVisit = findTodayVisit(user.userId(), place.getId(), today);
        boolean alreadyStamped = todayVisit.isPresent();
        Visit visit;
        if (alreadyStamped) {
            // 오늘 이미 찍었으면 지금 어디 있든 기존 도장을 돌려준다 (버튼·해설 두 경로로 다시 올 수 있다)
            visit = todayVisit.get();
        } else {
            requireNearby(request, place);
            try {
                visit = visitRepository.save(Visit.create(user.userId(), place.getId(), today));
            } catch (DataIntegrityViolationException e) {
                // 같은 요청이 동시에 들어와 다른 요청이 먼저 저장한 경우다
                visit = findTodayVisit(user.userId(), place.getId(), today).orElseThrow(() -> e);
                alreadyStamped = true;
            }
        }

        boolean firstVisit = !visitRepository.existsByUserIdAndPlaceIdAndVisitedDateBefore(
                user.userId(), place.getId(), today);
        Set<Long> visitedPlaceIds = new HashSet<>(visitRepository.findVisitedPlaceIds(user.userId()));
        visitedPlaceIds.add(place.getId());
        LocalTime time = request.simulatedTime() != null ? request.simulatedTime() : SeoulTime.now().toLocalTime();
        RecommendedCourseDto course = courseRecommender.recommend(
                place, firstVisit, visitedPlaceIds, user.language(), time, request.simulatedWeather());

        return new VisitResponseDto(visit.getId(), place.getId(), place.getName(), visit.getVisitedDate(),
                alreadyStamped, firstVisit, course);
    }

    /** 기간 내 방문자가 많은 장소 (F2 지난주 인기). 한 사람이 여러 날 와도 1명이다 */
    public List<PlaceVisitorCount> topPlaces(LocalDate from, LocalDate to, int limit) {
        return visitRepository.findTopPlaces(from, to, PageRequest.of(0, limit));
    }

    private Optional<Visit> findTodayVisit(String userId, Long placeId, LocalDate today) {
        return visitRepository.findByUserIdAndPlaceIdAndVisitedDate(userId, placeId, today);
    }

    private void requireNearby(VisitRequestDto request, Place place) {
        int distance = GeoUtils.distanceMeters(request.lat(), request.lng(), place.getLatitude(), place.getLongitude());
        if (distance > stampRadiusMeters) {
            throw new VisitException(VisitErrorType.TOO_FAR, "placeId=" + place.getId());
        }
    }
}
