package com.hackathon.domain.review.sample;

import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.service.PlaceService;
import com.hackathon.domain.review.entity.Review;
import com.hackathon.domain.review.repository.ReviewRepository;
import com.hackathon.domain.user.service.UserService;
import com.hackathon.global.common.Nationality;
import com.hackathon.global.util.SeoulTime;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * 샘플 한걸음 (F6). data/sample-reviews.json을 읽어 서버를 켤 때 넣는다.
 * - 쓴 사람은 국적별 샘플 사용자(app_user.sample=true, UserService.getOrCreateSampleUser)
 * - 장소는 contentId로 찾는다. 샘플 한걸음이 이미 있는 장소는 건너뛴다 (재시작해도 두 번 들어가지 않는다)
 * - 장소 동기화(DataSyncScheduler)가 시작 직후 따로 돌기 때문에, 아직 없는 장소는 잠시 뒤 다시 시도한다
 * - 파일 형식이 틀리면 서버가 뜨지 않는다. 샘플은 검사(moderate)를 거치지 않으므로 내용은 사람이 확인한다
 * - app.sample.reviews-enabled=false면 넣지 않는다 (테스트)
 */
@Slf4j
@Component
public class SampleReviewSeeder {

    static final String LOCATION = "data/sample-reviews.json";
    private static final int RETRIES = 15;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(20);

    private final PlaceService placeService;
    private final UserService userService;
    private final ReviewRepository reviewRepository;
    private final boolean enabled;
    /** contentId → 그 장소의 샘플 (파일 순서 유지, 내용이 빈 줄은 뺐다) */
    private final Map<String, List<SampleReviewFile.Entry>> byContentId;

    public SampleReviewSeeder(PlaceService placeService, UserService userService, ReviewRepository reviewRepository,
                              JsonMapper jsonMapper, @Value("${app.sample.reviews-enabled:true}") boolean enabled)
            throws IOException {
        this.placeService = placeService;
        this.userService = userService;
        this.reviewRepository = reviewRepository;
        this.enabled = enabled;
        this.byContentId = load(jsonMapper);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedOnStartup() {
        if (!enabled || byContentId.isEmpty()) return;
        // 샘플 사용자는 스레드를 띄우기 전에 이 스레드(시작 이벤트)에서 만든다.
        // 샘플 방문 시더도 같은 사용자를 쓰므로, 두 스레드가 동시에 "국적별 첫 번째 샘플 사용자"를 만들지 않게 한다
        byContentId.values().stream().flatMap(List::stream).map(SampleReviewFile.Entry::nationality).distinct()
                .forEach(userService::getOrCreateSampleUser);
        Thread.ofVirtual().name("sample-review-seed").start(() -> {
            for (int attempt = 1; attempt <= RETRIES; attempt++) {
                int pending = seed();
                if (pending == 0) return;
                if (attempt == RETRIES) {
                    log.info("샘플 한걸음: 장소 {}곳은 아직 없어 넣지 않았다 (장소 동기화 후 서버를 다시 켜면 넣는다)", pending);
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

    /** 넣을 수 있는 장소에 넣고, 장소가 아직 없어 못 넣은 장소 수를 돌려준다 */
    public int seed() {
        Map<Nationality, String> sampleUsers = new EnumMap<>(Nationality.class);
        int pending = 0;
        int inserted = 0;
        for (Map.Entry<String, List<SampleReviewFile.Entry>> group : byContentId.entrySet()) {
            Optional<Place> place = placeService.findActiveByContentId(group.getKey());
            if (place.isEmpty()) {
                pending++;
                continue;
            }
            for (SampleReviewFile.Entry entry : group.getValue()) {
                sampleUsers.computeIfAbsent(entry.nationality(), userService::getOrCreateSampleUser);
            }
            Long placeId = place.get().getId();
            if (reviewRepository.existsByPlaceIdAndUserIdIn(placeId, sampleUsers.values())) continue;

            for (SampleReviewFile.Entry entry : group.getValue()) {
                Review review = reviewRepository.save(Review.create(placeId, sampleUsers.get(entry.nationality()),
                        entry.content().strip(), entry.language()));
                int daysAgo = entry.daysAgo() == null ? 0 : entry.daysAgo();
                reviewRepository.backdate(review.getId(), SeoulTime.now().minusDays(daysAgo));
                inserted++;
            }
        }
        if (inserted > 0) log.info("샘플 한걸음 {}개를 넣었다", inserted);
        return pending;
    }

    private static Map<String, List<SampleReviewFile.Entry>> load(JsonMapper jsonMapper) throws IOException {
        ClassPathResource resource = new ClassPathResource(LOCATION);
        if (!resource.exists()) return Map.of();
        SampleReviewFile file;
        try (InputStream in = resource.getInputStream()) {
            file = jsonMapper.readValue(in, SampleReviewFile.class);
        }
        Map<String, List<SampleReviewFile.Entry>> result = new LinkedHashMap<>();
        if (file.reviews() == null) return result;
        for (SampleReviewFile.Entry entry : file.reviews()) {
            if (!entry.isFilled()) continue;
            validate(entry);
            result.computeIfAbsent(entry.contentId(), key -> new ArrayList<>()).add(entry);
        }
        return result;
    }

    private static void validate(SampleReviewFile.Entry entry) {
        String where = LOCATION + " (" + entry.contentId() + " " + entry.name() + "): ";
        if (entry.contentId() == null || entry.contentId().isBlank()) throw new IllegalStateException(where + "contentId가 없다");
        if (entry.nationality() == null) throw new IllegalStateException(where + "nationality가 없다");
        if (entry.language() == null) throw new IllegalStateException(where + "language가 없다");
        if (entry.content().strip().length() > Review.CONTENT_MAX) {
            throw new IllegalStateException(where + "content가 " + Review.CONTENT_MAX + "자를 넘는다");
        }
        if (entry.daysAgo() != null && (entry.daysAgo() < 0 || entry.daysAgo() > 30)) {
            throw new IllegalStateException(where + "daysAgo는 0~30이어야 한다");
        }
    }
}
