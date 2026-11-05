package com.hackathon.domain.visit.sample;

import com.hackathon.domain.place.entity.Place;
import com.hackathon.domain.place.service.PlaceService;
import com.hackathon.domain.user.service.UserService;
import com.hackathon.domain.visit.entity.Visit;
import com.hackathon.domain.visit.repository.VisitRepository;
import com.hackathon.global.common.Nationality;
import com.hackathon.global.util.SeoulTime;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * 샘플 방문 기록 (F5·F6). data/sample-visits.json을 읽어 서버를 켤 때 넣는다. 흔적 지도·지난주 인기가 비어 보이지 않게 한다.
 * - 방문자는 국적별 샘플 사용자(app_user.sample=true)다. 국적별 첫 번째 사용자는 샘플 한걸음 작성자와 같은 사람이다
 * - 서버를 켤 때마다 파일의 장소에서 샘플 사용자의 방문을 지우고 오늘 기준 1~10일 전으로 다시 넣는다 (날짜가 늘 최근이다)
 * - 장소는 contentId로 찾는다. 장소 동기화(DataSyncScheduler)가 시작 직후 따로 돌기 때문에, 아직 없는 장소는 잠시 뒤 다시 시도한다
 * - 파일 형식이 틀리면 서버가 뜨지 않는다. app.sample.visits-enabled=false면 넣지 않는다 (테스트)
 */
@Slf4j
@Component
public class SampleVisitSeeder {

    static final String LOCATION = "data/sample-visits.json";
    static final int MAX_VISITORS = 10;
    /** 방문 날짜를 1~DAYS_SPREAD일 전에 흩어 둔다 */
    static final int DAYS_SPREAD = 10;
    private static final int RETRIES = 15;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(20);

    private final PlaceService placeService;
    private final UserService userService;
    private final VisitRepository visitRepository;
    private final boolean enabled;
    private final List<SampleVisitFile.Entry> places;
    /** 국적별로 필요한 샘플 사용자 수 (파일에서 가장 많은 장소 기준) */
    private final Map<Nationality, Integer> usersNeeded;

    public SampleVisitSeeder(PlaceService placeService, UserService userService, VisitRepository visitRepository,
                             JsonMapper jsonMapper, @Value("${app.sample.visits-enabled:true}") boolean enabled)
            throws IOException {
        this.placeService = placeService;
        this.userService = userService;
        this.visitRepository = visitRepository;
        this.enabled = enabled;
        this.places = load(jsonMapper);
        Map<Nationality, Integer> needed = new EnumMap<>(Nationality.class);
        places.forEach(entry -> entry.visitors().forEach((nationality, count) -> needed.merge(nationality, count, Math::max)));
        this.usersNeeded = needed;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedOnStartup() {
        if (!enabled || places.isEmpty()) return;
        // 샘플 사용자는 스레드를 띄우기 전에 이 스레드(시작 이벤트)에서 만든다.
        // 샘플 한걸음 시더와 동시에 "국적별 첫 번째 샘플 사용자"를 만들지 않게 한다
        usersNeeded.forEach(userService::getOrCreateSampleUsers);
        Thread.ofVirtual().name("sample-visit-seed").start(() -> {
            for (int attempt = 1; attempt <= RETRIES; attempt++) {
                int pending = seed();
                if (pending == 0) return;
                if (attempt == RETRIES) {
                    log.info("샘플 방문: 장소 {}곳은 아직 없어 넣지 않았다 (장소 동기화 후 서버를 다시 켜면 넣는다)", pending);
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

    /**
     * 있는 장소마다 샘플 방문을 지우고 다시 넣는다. 장소가 아직 없어 못 넣은 장소 수를 돌려준다.
     * 여러 번 불러도 결과는 같다 (장소마다 국적별 방문자 수가 파일과 같다).
     */
    public int seed() {
        Map<Nationality, List<String>> sampleUsers = new EnumMap<>(Nationality.class);
        usersNeeded.forEach((nationality, count) ->
                sampleUsers.put(nationality, userService.getOrCreateSampleUsers(nationality, count)));
        Set<String> allSampleUsers = userService.getAllSampleUserIds();
        LocalDate today = SeoulTime.today();

        int pending = 0;
        int inserted = 0;
        for (int index = 0; index < places.size(); index++) {
            SampleVisitFile.Entry entry = places.get(index);
            Optional<Place> place = placeService.findActiveByContentId(entry.contentId());
            if (place.isEmpty()) {
                pending++;
                continue;
            }
            Long placeId = place.get().getId();
            visitRepository.deleteByPlaceIdAndUserIdIn(placeId, allSampleUsers);

            List<Visit> visits = new ArrayList<>();
            for (Map.Entry<Nationality, Integer> visitor : entry.visitors().entrySet()) {
                List<String> users = sampleUsers.get(visitor.getKey());
                for (int i = 0; i < visitor.getValue(); i++) {
                    visits.add(Visit.create(users.get(i), placeId,
                            today.minusDays(daysAgo(i, index, visitor.getKey()))));
                }
            }
            visitRepository.saveAll(visits);
            inserted += visits.size();
        }
        if (inserted > 0) log.info("샘플 방문 {}건을 넣었다 (장소 {}곳)", inserted, places.size() - pending);
        return pending;
    }

    /** 1~DAYS_SPREAD일 전. 사람·장소·국적마다 다르게 흩어 둔다 (늘 같은 값이다) */
    static int daysAgo(int visitorIndex, int placeIndex, Nationality nationality) {
        return 1 + (visitorIndex * 3 + placeIndex * 2 + nationality.ordinal()) % DAYS_SPREAD;
    }

    private static List<SampleVisitFile.Entry> load(JsonMapper jsonMapper) throws IOException {
        ClassPathResource resource = new ClassPathResource(LOCATION);
        if (!resource.exists()) return List.of();
        SampleVisitFile file;
        try (InputStream in = resource.getInputStream()) {
            file = jsonMapper.readValue(in, SampleVisitFile.class);
        }
        if (file.places() == null) return List.of();
        file.places().forEach(SampleVisitSeeder::validate);
        return List.copyOf(file.places());
    }

    private static void validate(SampleVisitFile.Entry entry) {
        String where = LOCATION + " (" + entry.contentId() + " " + entry.name() + "): ";
        if (entry.contentId() == null || entry.contentId().isBlank()) throw new IllegalStateException(where + "contentId가 없다");
        if (entry.visitors() == null || entry.visitors().isEmpty()) throw new IllegalStateException(where + "visitors가 없다");
        entry.visitors().forEach((nationality, count) -> {
            if (nationality == null) throw new IllegalStateException(where + "국적 코드가 올바르지 않다");
            if (count == null || count < 1 || count > MAX_VISITORS) {
                throw new IllegalStateException(where + nationality + " 방문자 수는 1~" + MAX_VISITORS + "이어야 한다");
            }
        });
    }
}
