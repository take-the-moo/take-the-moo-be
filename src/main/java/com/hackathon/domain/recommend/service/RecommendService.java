package com.hackathon.domain.recommend.service;

import com.hackathon.domain.llm.exception.LlmException;
import com.hackathon.domain.llm.prompt.LlmPrompt;
import com.hackathon.domain.llm.prompt.PromptRegistry;
import com.hackathon.domain.llm.service.LlmClient;
import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.entity.Station;
import com.hackathon.domain.place.service.BaseLocation;
import com.hackathon.domain.place.service.NearbyPlace;
import com.hackathon.domain.place.service.PlaceService;
import com.hackathon.domain.place.service.StationService;
import com.hackathon.domain.recommend.dto.response.NowResponseDto;
import com.hackathon.domain.recommend.dto.response.RecommendChoice;
import com.hackathon.domain.recommend.dto.response.StationResponseDto;
import com.hackathon.domain.recommend.exception.RecommendErrorType;
import com.hackathon.domain.recommend.exception.RecommendException;
import com.hackathon.domain.user.dto.response.UserResponseDto;
import com.hackathon.domain.user.service.UserService;
import com.hackathon.domain.visit.service.VisitService;
import com.hackathon.domain.weather.dto.CurrentWeather;
import com.hackathon.domain.weather.dto.WeatherCondition;
import com.hackathon.domain.weather.service.WeatherService;
import com.hackathon.global.common.Language;
import com.hackathon.global.common.Nationality;
import com.hackathon.global.common.TimeSlot;
import com.hackathon.global.util.SeoulTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/**
 * F2 지금 추천. 장소·역 데이터는 domain/place의 서비스로만 읽는다. 추천 결과는 저장하지 않는다.
 * 1. 규칙으로 후보를 거른다 (AI 없음): 노출 중 + 해설 원문 있음 → 지금 시간대 → 비·눈이면 실내 → 가까운 순 CANDIDATES곳
 *    시간대·실내 조건은 맞는 후보가 하나도 없으면 뺀다 (밤 21–06시는 시간대가 없어 처음부터 뺀다)
 * 2. AI(recommend)가 후보 중 1곳과 이유를 고른다. 후보 밖 placeId·빈 이유·AI 실패면 가장 가까운 후보와 기본 문구
 * 위치(lat/lng)는 계산에만 쓰고 AI 입력·로그·응답에 넣지 않는다 (AI에는 거리만 간다).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendService {

    private static final String PROMPT_KEY = "recommend";
    static final int CANDIDATES = 6;
    static final int ALTERNATIVES = 3;
    static final int POPULAR = 3;
    /** 지난주 인기 기간 (오늘 포함) */
    static final int POPULAR_DAYS = 7;
    static final int SUMMARY_MAX = 300;
    /** 걷는 속도 (분당 미터) */
    static final int WALK_METERS_PER_MINUTE = 67;
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final StationService stationService;
    private final PlaceService placeService;
    private final UserService userService;
    private final WeatherService weatherService;
    private final VisitService visitService;
    private final PromptRegistry promptRegistry;
    private final LlmClient llmClient;
    private final JsonMapper jsonMapper;

    /** 역 선택 목록. 기본 역(GET /api/now가 위치 없이 쓰는 역)에 isDefault를 붙인다 */
    public List<StationResponseDto> getStations() {
        Long defaultId = stationService.findDefaultStation().map(Station::getId).orElse(null);
        return stationService.getSelectableStations().stream()
                .map(station -> StationResponseDto.of(station, station.getId().equals(defaultId)))
                .toList();
    }

    /**
     * 지금 추천. 모든 파라미터는 선택이다 (발표장이 실내라 위치·날씨·시간을 지정할 수 있다).
     * userId가 없으면 국적 OTHER·언어 EN으로 쓴다.
     */
    public NowResponseDto now(String userId, Double lat, Double lng, Long stationId,
                              WeatherCondition simulatedWeather, LocalTime simulatedTime) {
        Nationality nationality = Nationality.OTHER;
        Language language = Nationality.OTHER.getDefaultLanguage();
        if (userId != null && !userId.isBlank()) {
            UserResponseDto user = userService.getUser(userId);
            nationality = user.nationality();
            language = user.language();
        }

        BaseLocation location = stationService.resolve(lat, lng, stationId);
        LocalTime time = simulatedTime != null ? simulatedTime : SeoulTime.now().toLocalTime();
        Optional<TimeSlot> timeSlot = TimeSlot.from(time);
        CurrentWeather weather = weatherService.resolve(simulatedWeather);

        List<NearbyPlace> activePlaces = placeService.getActivePlacesByDistance(location);
        List<NearbyPlace> candidates = candidates(activePlaces, timeSlot.orElse(null), weather.condition());
        if (candidates.isEmpty()) {
            throw new RecommendException(RecommendErrorType.NOT_FOUND, "노출 중이고 해설이 있는 장소 없음");
        }

        RecommendChoice choice = choose(candidates, nationality, language, time, timeSlot.orElse(null), weather);
        NearbyPlace chosen = candidates.stream()
                .filter(candidate -> candidate.place().getId().equals(choice.placeId()))
                .findFirst()
                .orElse(null);
        String reason = choice.reason();
        String reasonKo = choice.reasonKo();
        if (chosen == null) {
            log.warn("추천: AI가 후보 밖 장소를 골라 가장 가까운 후보로 바꾼다 (placeId={})", choice.placeId());
            chosen = candidates.getFirst();
            reason = null;
        }
        if (isBlank(reason) || isBlank(reasonKo)) {
            reason = RecommendFallback.reason(language);
            reasonKo = RecommendFallback.REASON_KO;
        }

        NearbyPlace picked = chosen;
        List<NowResponseDto.Alternative> alternatives = candidates.stream()
                .filter(candidate -> candidate != picked)
                .limit(ALTERNATIVES)
                .map(candidate -> new NowResponseDto.Alternative(candidate.place().getId(),
                        candidate.place().getName(), candidate.distanceMeters()))
                .toList();

        return new NowResponseDto(
                recommendedPlace(picked),
                reason.strip(),
                reasonKo.strip(),
                new NowResponseDto.Context(time.format(HH_MM), timeSlot.orElse(null), weather.condition(),
                        weather.temperature(), weather.source(), location.baseStation()),
                alternatives,
                weeklyPopular(activePlaces));
    }

    /**
     * 지난주 인기: 오늘을 포함한 최근 7일(서울 날짜) 동안 방문자가 많은 노출 장소 POPULAR곳.
     * 방문자는 사람 수다 (한 사람이 여러 날 와도 1명). 노출하지 않는 장소는 뺀다. 날짜까지만 쓰고 개인 방문 시각은 내보내지 않는다
     */
    private List<NowResponseDto.PopularPlace> weeklyPopular(List<NearbyPlace> activePlaces) {
        Map<Long, Place> byId = activePlaces.stream()
                .collect(Collectors.toMap(nearby -> nearby.place().getId(), NearbyPlace::place));
        LocalDate today = SeoulTime.today();
        // 노출하지 않는 장소가 섞여도 POPULAR곳을 채울 수 있게 넉넉히 받는다
        return visitService.topPlaces(today.minusDays(POPULAR_DAYS - 1), today, POPULAR * 2).stream()
                .filter(count -> byId.containsKey(count.placeId()))
                .limit(POPULAR)
                .map(count -> new NowResponseDto.PopularPlace(count.placeId(), byId.get(count.placeId()).getName(),
                        count.visitorCount()))
                .toList();
    }

    /**
     * 규칙으로 후보를 거른다. places는 가까운 순이어야 한다.
     * 해설 원문이 없는 장소는 늘 뺀다. 시간대(null이면 조건 없음)·실내 조건은 맞는 곳이 없으면 뺀다.
     */
    static List<NearbyPlace> candidates(List<NearbyPlace> places, TimeSlot timeSlot, WeatherCondition weather) {
        List<NearbyPlace> result = places.stream()
                .filter(nearby -> nearby.place().fieldExplanation() != null)
                .toList();
        if (timeSlot != null) {
            result = filterOrKeep(result, nearby -> nearby.place().getTimeSlots().contains(timeSlot));
        }
        if (weather != null && weather.prefersIndoor()) {
            result = filterOrKeep(result, nearby -> Boolean.TRUE.equals(nearby.place().getIndoor()));
        }
        return result.stream().limit(CANDIDATES).toList();
    }

    private static List<NearbyPlace> filterOrKeep(List<NearbyPlace> places, Predicate<NearbyPlace> condition) {
        List<NearbyPlace> filtered = places.stream().filter(condition).toList();
        return filtered.isEmpty() ? places : filtered;
    }

    /** AI가 1곳을 고른다. AI 호출이 실패하면 빈 선택(기본 문구로 이어진다) */
    private RecommendChoice choose(List<NearbyPlace> candidates, Nationality nationality, Language language,
                                   LocalTime time, TimeSlot timeSlot, CurrentWeather weather) {
        LlmPrompt prompt = promptRegistry.resolve(PROMPT_KEY, Map.of(
                "nationality", nationality.getKoreanName(),
                "language", language.getKoreanName()));
        String input = input(candidates, nationality, time, timeSlot, weather);
        NearbyPlace first = candidates.getFirst();
        try {
            RecommendChoice choice = llmClient.entity(prompt, input, RecommendChoice.class,
                    () -> RecommendChoice.mock(first.place().getId(), first.place().getName(), nationality, language));
            return choice != null ? choice : new RecommendChoice(null, null, null);
        } catch (LlmException e) {
            log.warn("추천: AI 호출 실패, 가장 가까운 후보와 기본 문구를 쓴다 ({})", e.getErrorCode().getCode());
            return new RecommendChoice(null, null, null);
        }
    }

    /**
     * user 메시지로 넘기는 상황과 후보. 필드 의미는 prompts.yml recommend.system에 적혀 있다. 좌표는 넣지 않는다.
     * connection은 사람이 검토한 내 나라 연결(없으면 null) — F3 장면의 한 줄 연결과 같은 값이라 두 화면의 말이 맞는다
     */
    private String input(List<NearbyPlace> candidates, Nationality nationality, LocalTime time, TimeSlot timeSlot,
                         CurrentWeather weather) {
        Map<String, Object> situation = new LinkedHashMap<>();
        situation.put("time", time.format(HH_MM));
        situation.put("timeSlot", timeSlot);
        situation.put("weather", weather.condition());
        situation.put("temperature", weather.temperature());

        List<Map<String, Object>> items = candidates.stream().map(candidate -> {
            Place place = candidate.place();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("placeId", place.getId());
            item.put("name", place.getName());
            item.put("indoor", Boolean.TRUE.equals(place.getIndoor()));
            item.put("sensitiveHistory", place.isSensitiveHistory());
            item.put("distanceMeters", candidate.distanceMeters());
            item.put("summary", truncate(place.fieldExplanation(), SUMMARY_MAX));
            item.put("connection", placeService.findConnection(place, nationality).orElse(null));
            return item;
        }).toList();

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("situation", situation);
        input.put("candidates", items);
        return jsonMapper.writeValueAsString(input);
    }

    private static NowResponseDto.RecommendedPlace recommendedPlace(NearbyPlace nearby) {
        Place place = nearby.place();
        return new NowResponseDto.RecommendedPlace(place.getId(), place.getName(), place.getImageUrl(),
                nearby.distanceMeters(), walkMinutes(nearby.distanceMeters()), Boolean.TRUE.equals(place.getIndoor()));
    }

    /** 거리 ÷ 분당 67m, 올림. 최소 1분 */
    static int walkMinutes(int distanceMeters) {
        return Math.max(1, (distanceMeters + WALK_METERS_PER_MINUTE - 1) / WALK_METERS_PER_MINUTE);
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
