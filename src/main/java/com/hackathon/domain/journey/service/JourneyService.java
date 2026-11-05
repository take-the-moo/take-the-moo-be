package com.hackathon.domain.journey.service;

import com.hackathon.domain.journey.dto.response.JourneyResponseDto;
import com.hackathon.domain.journey.dto.response.JourneyResponseDto.Moment;
import com.hackathon.domain.journey.dto.response.JourneyResponseDto.Stats;
import com.hackathon.domain.journey.dto.response.MomentType;
import com.hackathon.domain.photo.service.PhotoInfo;
import com.hackathon.domain.photo.service.PhotoService;
import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.service.PlaceImageService;
import com.hackathon.domain.place.service.PlaceService;
import com.hackathon.domain.review.service.ReviewService;
import com.hackathon.domain.review.service.UserReview;
import com.hackathon.domain.visit.dto.response.VisitHistoryResponseDto.Stamp;
import com.hackathon.domain.visit.service.JourneyVisits;
import com.hackathon.domain.visit.service.VisitHistoryService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * F8 나의 발자취: 기간, 숫자, 순간 카드. AI를 부르지 않아 바로 응답한다 (이야기는 JourneyStoryService).
 * 찾을 수 없는 장소의 기록은 나의 기록처럼 뺀다.
 */
@Service
@RequiredArgsConstructor
public class JourneyService {

    private final VisitHistoryService visitHistoryService;
    private final ReviewService reviewService;
    private final PhotoService photoService;
    private final PlaceService placeService;

    public JourneyResponseDto getJourney(String userId) {
        JourneyRecords records = loadRecords(userId);
        JourneyVisits visits = records.visits();
        Stats stats = new Stats(visits.stamps().size(), visits.completed().size(), records.reviews().size());
        return new JourneyResponseDto(records.startDate(), records.endDate(), stats,
                moments(records, firstPhotoByPlace(userId)));
    }

    /** 토큰 확인은 getJourneyVisits 안의 UserService.getUser가 한다. 그래서 가장 먼저 부른다 */
    JourneyRecords loadRecords(String userId) {
        JourneyVisits visits = visitHistoryService.getJourneyVisits(userId);
        List<UserReview> reviews = reviewService.getUserReviews(userId);
        Set<Long> placeIds = new HashSet<>();
        visits.stamps().forEach(stamp -> placeIds.add(stamp.placeId()));
        reviews.forEach(review -> placeIds.add(review.placeId()));
        Map<Long, Place> places = placeService.getPlacesByIds(placeIds).stream()
                .collect(Collectors.toMap(Place::getId, Function.identity()));
        List<UserReview> known = reviews.stream().filter(review -> places.containsKey(review.placeId())).toList();
        return new JourneyRecords(visits, known, places);
    }

    private static List<Moment> moments(JourneyRecords records, Map<Long, String> photos) {
        JourneyVisits visits = records.visits();
        Map<Long, Place> places = records.places();
        List<Moment> moments = new ArrayList<>();
        if (!visits.stamps().isEmpty()) {
            Stamp first = visits.stamps().getLast();
            moments.add(new Moment(MomentType.FIRST_STAMP, first.firstVisitedDate(), first.placeId(), first.name(),
                    null, null, placeImage(first.placeId(), places, photos)));
        }
        for (UserReview review : records.reviews()) {
            String image = review.photoId() != null ? PhotoService.urlOf(review.photoId())
                    : placeImage(review.placeId(), places, photos);
            moments.add(new Moment(MomentType.REVIEW, review.date(), review.placeId(),
                    places.get(review.placeId()).getName(), null, review.content(), image));
        }
        for (JourneyVisits.CompletedCourse course : visits.completed()) {
            moments.add(new Moment(MomentType.COURSE_COMPLETED, course.date(), course.placeId(), course.placeName(),
                    course.name(), null, placeImage(course.placeId(), places, photos)));
        }
        // 안정 정렬이라 같은 날 같은 종류는 넣은 순서를 유지한다 (한걸음은 오래된 순)
        moments.sort(Comparator.comparing(Moment::date).thenComparing(Moment::type));
        return moments;
    }

    /** 그 장소의 내 사진 → 장소 사진 */
    private static String placeImage(Long placeId, Map<Long, Place> places, Map<Long, String> photos) {
        String photo = photos.get(placeId);
        return photo != null ? photo : PlaceImageService.urlOf(places.get(placeId));
    }

    /** 장소별로 내가 가장 먼저 올린 사진 주소 */
    private Map<Long, String> firstPhotoByPlace(String userId) {
        Map<Long, String> result = new HashMap<>();
        for (PhotoInfo photo : photoService.getUserPhotos(userId)) {
            if (photo.placeId() != null) result.putIfAbsent(photo.placeId(), photo.url());
        }
        return result;
    }
}
