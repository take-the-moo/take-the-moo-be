package com.hackathon.domain.place.curation;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * data/place-curation.json(팀 보강 데이터)을 시작할 때 읽어 contentId로 찾게 한다.
 * 파일 형식이 틀리면 서버가 뜨지 않는다 (잘못된 해설이 조용히 빠지는 것보다 낫다).
 */
@Slf4j
@Component
public class PlaceCurationRegistry {

    static final String LOCATION = "data/place-curation.json";

    private final Map<String, PlaceCuration> byContentId;

    public PlaceCurationRegistry(JsonMapper jsonMapper) throws IOException {
        ClassPathResource resource = new ClassPathResource(LOCATION);
        Map<String, PlaceCuration> loaded = new LinkedHashMap<>();
        if (resource.exists()) {
            try (InputStream in = resource.getInputStream()) {
                Arrays.stream(jsonMapper.readValue(in, PlaceCuration[].class))
                        .filter(c -> c.contentId() != null && !c.contentId().isBlank())
                        .forEach(c -> loaded.put(c.contentId(), c));
            }
        }
        this.byContentId = Map.copyOf(loaded);
        log.info("장소 보강 데이터 {}건을 읽었습니다 ({})", byContentId.size(), LOCATION);
    }

    public Optional<PlaceCuration> find(String contentId) {
        return Optional.ofNullable(byContentId.get(contentId));
    }

    public int size() {
        return byContentId.size();
    }
}
