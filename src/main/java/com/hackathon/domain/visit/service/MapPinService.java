package com.hackathon.domain.visit.service;

import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.service.PlaceService;
import com.hackathon.domain.review.service.ReviewService;
import com.hackathon.domain.user.service.UserService;
import com.hackathon.domain.visit.dto.response.MapPinResponseDto;
import com.hackathon.domain.visit.dto.response.PlaceVisitor;
import com.hackathon.domain.visit.repository.VisitRepository;
import com.hackathon.global.common.Nationality;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * F6 흔적 지도 — "먼저 온 유학생의 발자취". 노출 중인 장소마다 다녀간 사람 수·국적 분포·한걸음 수를 준다.
 * - 방문자는 방문 도장(visit) 기준 사람 수다. 국적은 사람별로 모은 뒤 UserService로 한 번에 조회해 합친다 (docs/db.md)
 * - 샘플 사용자의 방문·한걸음도 센다 (지도가 비어 보이지 않게 넣은 데이터다)
 * - 장소 단위 집계만 내보낸다. 개인의 방문 날짜·시각과 사용자 ID는 응답에 넣지 않는다
 */
@Service
@RequiredArgsConstructor
public class MapPinService {

    private final PlaceService placeService;
    private final ReviewService reviewService;
    private final UserService userService;
    private final VisitRepository visitRepository;

    /** 노출 중인 장소 전부. 방문자 많은 순 → 한걸음 많은 순 → placeId 순 */
    public List<MapPinResponseDto> getPins() {
        Map<Long, Set<String>> visitorsByPlace = visitRepository.findPlaceVisitors().stream()
                .collect(Collectors.groupingBy(PlaceVisitor::placeId,
                        Collectors.mapping(PlaceVisitor::userId, Collectors.toSet())));
        Set<String> allVisitors = visitorsByPlace.values().stream()
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
        Map<String, Nationality> nationalities = userService.getNationalities(allVisitors);
        Map<Long, Long> reviewCounts = reviewService.countByPlace();

        return placeService.getActivePlaces().stream()
                .map(place -> pin(place, visitorsByPlace.getOrDefault(place.getId(), Set.of()), nationalities,
                        reviewCounts.getOrDefault(place.getId(), 0L)))
                .sorted(Comparator.comparingLong(MapPinResponseDto::visitorCount).reversed()
                        .thenComparing(Comparator.comparingLong(MapPinResponseDto::reviewCount).reversed())
                        .thenComparing(MapPinResponseDto::placeId))
                .toList();
    }

    private static MapPinResponseDto pin(Place place, Set<String> visitors, Map<String, Nationality> nationalities,
                                         long reviewCount) {
        Map<Nationality, Long> counts = new EnumMap<>(Nationality.class);
        for (String userId : visitors) {
            Nationality nationality = nationalities.get(userId);
            // 탈퇴 개념은 없지만, 사용자 행이 없으면 국적 분포에서만 뺀다 (방문자 수에는 남는다)
            if (nationality != null) counts.merge(nationality, 1L, Long::sum);
        }
        List<MapPinResponseDto.NationalityCount> distribution = counts.entrySet().stream()
                .sorted(Map.Entry.<Nationality, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> new MapPinResponseDto.NationalityCount(entry.getKey(), entry.getValue()))
                .toList();
        return new MapPinResponseDto(place.getId(), place.getName(), place.getLatitude(), place.getLongitude(),
                place.getImageUrl(), visitors.size(), reviewCount, distribution);
    }
}
