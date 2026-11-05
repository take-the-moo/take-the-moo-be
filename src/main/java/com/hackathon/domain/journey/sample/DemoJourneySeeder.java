package com.hackathon.domain.journey.sample;

import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.service.PlaceService;
import com.hackathon.domain.review.entity.Review;
import com.hackathon.domain.review.service.ReviewService;
import com.hackathon.domain.user.exception.UserException;
import com.hackathon.domain.user.service.UserService;
import com.hackathon.domain.visit.service.VisitService;
import com.hackathon.global.common.Language;
import com.hackathon.global.common.Nationality;
import com.hackathon.global.config.AppProperties;
import com.hackathon.global.util.SeoulTime;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * 시연 계정 (F8). 샘플 사용자는 로그인할 수 없어 나의 발자취를 보여 줄 수 없으므로 로그인되는 계정을 따로 만든다.
 * - 비밀번호(app.demo.password)가 없으면 아무것도 하지 않는다. 코드와 파일에 비밀번호를 쓰지 않는다
 * - 비밀번호까지 맞는 계정에만 넣는다. 다른 사람이 같은 아이디를 쓰고 있으면 경고만 남긴다
 * - 이미 도장이 있으면 다시 넣지 않는다 (재시작해도 중복되지 않는다)
 * - 장소는 contentId로 찾는다. 장소 동기화가 시작 직후 따로 돌기 때문에, 장소가 다 모일 때까지 기다렸다 한 번에 넣는다
 * - 일반 사용자로 집계되므로 지난주 인기에 섞이지 않게 도장은 15일 이상 전으로 넣는다 (파일이 틀리면 서버가 뜨지 않는다)
 */
@Slf4j
@Component
public class DemoJourneySeeder {

    static final String LOCATION = "data/demo-journey.json";
    static final String LOGIN_ID = "demo_linh";
    private static final String NAME = "린";
    private static final Language LANGUAGE = Language.VI;
    private static final int MIN_DAYS_AGO = 15;
    private static final int RETRIES = 15;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(20);

    private final UserService userService;
    private final PlaceService placeService;
    private final VisitService visitService;
    private final ReviewService reviewService;
    private final String password;
    private final DemoJourneyFile file;

    public DemoJourneySeeder(UserService userService, PlaceService placeService, VisitService visitService,
                             ReviewService reviewService, AppProperties properties, JsonMapper jsonMapper)
            throws IOException {
        this.userService = userService;
        this.placeService = placeService;
        this.visitService = visitService;
        this.reviewService = reviewService;
        this.password = properties.demo().password();
        this.file = load(jsonMapper);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedOnStartup() {
        if (password.isBlank()) return;
        Thread.ofVirtual().name("demo-journey-seed").start(() -> {
            for (int attempt = 1; attempt <= RETRIES; attempt++) {
                if (seed()) return;
                if (attempt == RETRIES) {
                    log.info("시연 계정: 장소가 아직 없어 기록을 넣지 않았다 (장소 동기화 후 서버를 다시 켜면 넣는다)");
                    return;
                }
                try {
                    Thread.sleep(RETRY_DELAY);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
    }

    /** 할 일이 끝났으면 true, 장소가 아직 모이지 않아 다시 시도해야 하면 false */
    public boolean seed() {
        if (password.isBlank() || !ensureAccount()) return true;
        if (visitService.hasVisits(LOGIN_ID)) return true;

        Map<String, Long> places = new HashMap<>();
        Stream<String> contentIds = Stream.concat(file.visits().stream().map(DemoJourneyFile.Visit::contentId),
                file.reviews().stream().map(DemoJourneyFile.Review::contentId));
        for (String contentId : contentIds.distinct().toList()) {
            Optional<Place> place = placeService.findActiveByContentId(contentId);
            if (place.isEmpty()) return false;
            places.put(contentId, place.get().getId());
        }
        for (DemoJourneyFile.Visit visit : file.visits()) {
            visitService.addDemoVisit(LOGIN_ID, places.get(visit.contentId()),
                    SeoulTime.today().minusDays(visit.daysAgo()));
        }
        for (DemoJourneyFile.Review review : file.reviews()) {
            reviewService.addDemoReview(LOGIN_ID, places.get(review.contentId()), review.content().strip(), LANGUAGE,
                    SeoulTime.now().minusDays(review.daysAgo()));
        }
        log.info("시연 계정 기록을 넣었다: 도장 {}개, 한걸음 {}개", file.visits().size(), file.reviews().size());
        return true;
    }

    /** 시연 계정을 쓸 수 있으면 true. 같은 아이디의 다른 계정이 있으면 건드리지 않는다 */
    private boolean ensureAccount() {
        if (userService.isLoginIdAvailable(LOGIN_ID)) {
            userService.signUp(LOGIN_ID, password, NAME, Nationality.VN, LANGUAGE);
            return true;
        }
        try {
            userService.checkCredentials(LOGIN_ID, password);
            return true;
        } catch (UserException e) {
            log.warn("시연 계정을 건너뛴다: 같은 아이디가 있는데 비밀번호가 맞지 않는다");
            return false;
        }
    }

    private static DemoJourneyFile load(JsonMapper jsonMapper) throws IOException {
        DemoJourneyFile file;
        try (InputStream in = new ClassPathResource(LOCATION).getInputStream()) {
            file = jsonMapper.readValue(in, DemoJourneyFile.class);
        }
        if (file.visits() == null || file.reviews() == null) {
            throw new IllegalStateException(LOCATION + ": visits와 reviews가 있어야 한다");
        }
        file.visits().forEach(visit -> validate(visit.contentId(), visit.daysAgo()));
        file.reviews().forEach(review -> {
            validate(review.contentId(), review.daysAgo());
            if (review.content() == null || review.content().isBlank() || review.content().length() > Review.CONTENT_MAX) {
                throw new IllegalStateException(LOCATION + " (" + review.contentId() + "): content 길이가 맞지 않다");
            }
        });
        return file;
    }

    private static void validate(String contentId, Integer daysAgo) {
        if (contentId == null || contentId.isBlank()) throw new IllegalStateException(LOCATION + ": contentId가 없다");
        if (daysAgo == null || daysAgo < MIN_DAYS_AGO) {
            throw new IllegalStateException(LOCATION + " (" + contentId + "): daysAgo는 " + MIN_DAYS_AGO + " 이상이어야 한다");
        }
    }
}
