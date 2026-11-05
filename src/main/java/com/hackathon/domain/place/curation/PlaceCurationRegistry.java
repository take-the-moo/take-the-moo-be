package com.hackathon.domain.place.curation;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * data/place-curation.json(팀 보강 데이터)을 시작할 때 읽는다. 파일을 고친 뒤 서버를 다시 켜면
 * 시작 시 동기화(DataSyncScheduler)가 새 규칙으로 장소 상태를 다시 계산한다.
 * 파일 형식이 틀리면 서버가 뜨지 않는다 (잘못된 해설이 조용히 빠지는 것보다 낫다).
 */
@Slf4j
@Component
public class PlaceCurationRegistry {

    static final String LOCATION = "data/place-curation.json";

    private final Map<String, PlaceCuration> byContentId;
    private final Set<String> excluded;

    @Autowired
    public PlaceCurationRegistry(JsonMapper jsonMapper) throws IOException {
        this(load(jsonMapper));
    }

    PlaceCurationRegistry(PlaceCurationFile file) {
        Map<String, PlaceCuration> places = new LinkedHashMap<>();
        nullToEmpty(file.places()).stream()
                .filter(c -> c.contentId() != null && !c.contentId().isBlank())
                .forEach(c -> places.put(c.contentId(), c));
        this.byContentId = Map.copyOf(places);
        this.excluded = Set.copyOf(nullToEmpty(file.excluded()));
        log.info("장소 보강 데이터: 보강 {}건, 제외 {}건 ({})", byContentId.size(), excluded.size(), LOCATION);
    }

    public Optional<PlaceCuration> find(String contentId) {
        return Optional.ofNullable(byContentId.get(contentId));
    }

    public boolean isExcluded(String contentId) {
        return excluded.contains(contentId);
    }

    public int size() {
        return byContentId.size();
    }

    private static PlaceCurationFile load(JsonMapper jsonMapper) throws IOException {
        ClassPathResource resource = new ClassPathResource(LOCATION);
        if (!resource.exists()) return new PlaceCurationFile(List.of(), List.of(), List.of());
        try (InputStream in = resource.getInputStream()) {
            return jsonMapper.readValue(in, PlaceCurationFile.class);
        }
    }

    private static <T> List<T> nullToEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }
}
