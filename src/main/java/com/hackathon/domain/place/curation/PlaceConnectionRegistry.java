package com.hackathon.domain.place.curation;

import com.hackathon.global.common.Nationality;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * data/place-connections.json(내 나라 연결, 사람이 검토한 데이터)을 시작할 때 읽는다. 고친 뒤 서버를 다시 켜면 반영된다.
 * 형식이 틀리면(없는 국적 키, OTHER, 빈 link, 같은 contentId 두 번) 서버가 뜨지 않는다.
 */
@Slf4j
@Component
public class PlaceConnectionRegistry {

    static final String LOCATION = "data/place-connections.json";

    private final Map<String, Map<Nationality, PlaceConnection>> byContentId;

    @Autowired
    public PlaceConnectionRegistry(JsonMapper jsonMapper) throws IOException {
        this(load(jsonMapper));
    }

    PlaceConnectionRegistry(PlaceConnectionFile file) {
        Map<String, Map<Nationality, PlaceConnection>> places = new HashMap<>();
        int count = 0;
        for (PlaceConnectionFile.Entry entry : nullToEmpty(file.places())) {
            if (entry.contentId() == null || entry.contentId().isBlank()) {
                throw new IllegalStateException(LOCATION + ": contentId가 비어 있다 (name=" + entry.name() + ")");
            }
            Map<Nationality, PlaceConnection> connections = new EnumMap<>(Nationality.class);
            if (entry.connections() != null) {
                entry.connections().forEach((nationality, connection) -> {
                    validate(entry, nationality, connection);
                    connections.put(nationality, normalize(connection));
                });
            }
            if (places.put(entry.contentId(), Map.copyOf(connections)) != null) {
                throw new IllegalStateException(LOCATION + ": contentId " + entry.contentId() + "가 두 번 있다");
            }
            count += connections.size();
        }
        this.byContentId = Map.copyOf(places);
        log.info("내 나라 연결 데이터: 장소 {}곳, 연결 {}건 ({})", byContentId.size(), count, LOCATION);
    }

    /** 장소(contentId) × 국적의 연결. 없는 조합·OTHER면 빈 값 */
    public Optional<PlaceConnection> find(String contentId, Nationality nationality) {
        if (contentId == null || nationality == null) return Optional.empty();
        return Optional.ofNullable(byContentId.getOrDefault(contentId, Map.of()).get(nationality));
    }

    private static void validate(PlaceConnectionFile.Entry entry, Nationality nationality, PlaceConnection connection) {
        String where = LOCATION + ": contentId " + entry.contentId() + " × " + nationality;
        if (nationality == null || nationality == Nationality.OTHER) {
            throw new IllegalStateException(where + ": 국적은 OTHER가 아닌 값이어야 한다");
        }
        if (connection == null || connection.link() == null || connection.link().isBlank()) {
            throw new IllegalStateException(where + ": link가 비어 있다");
        }
    }

    private static PlaceConnection normalize(PlaceConnection connection) {
        String counterpart = blankToNull(connection.counterpart());
        // 사례가 없으면 현지어 이름도 쓰지 않는다
        String localName = counterpart == null ? null : blankToNull(connection.localName());
        return new PlaceConnection(counterpart, localName, connection.link().strip());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static PlaceConnectionFile load(JsonMapper jsonMapper) throws IOException {
        ClassPathResource resource = new ClassPathResource(LOCATION);
        if (!resource.exists()) return new PlaceConnectionFile(List.of());
        try (InputStream in = resource.getInputStream()) {
            return jsonMapper.readValue(in, PlaceConnectionFile.class);
        }
    }

    private static <T> List<T> nullToEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }
}
