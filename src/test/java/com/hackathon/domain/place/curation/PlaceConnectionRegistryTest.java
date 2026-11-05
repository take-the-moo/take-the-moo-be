package com.hackathon.domain.place.curation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hackathon.global.common.Nationality;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class PlaceConnectionRegistryTest {

    /** 실제 data/place-connections.json이 읽히는지 (형식이 깨지면 서버가 안 뜨므로 테스트에서 먼저 잡는다) */
    @Test
    void 연결_파일을_읽는다() throws Exception {
        PlaceConnectionRegistry registry = new PlaceConnectionRegistry(JsonMapper.builder().build());

        // 서대문형무소역사관
        assertThat(registry.find("130152", Nationality.VN)).get()
                .extracting(PlaceConnection::counterpart, PlaceConnection::localName)
                .containsExactly("하노이 호아로 수용소", "Nhà tù Hỏa Lò");
        // 가해 쪽으로 얽힌 국적은 사례 이름 없이 방향만
        PlaceConnection japan = registry.find("130152", Nationality.JP).orElseThrow();
        assertThat(japan.counterpart()).isNull();
        assertThat(japan.link()).isNotBlank();
        // 없는 조합·OTHER·없는 장소
        assertThat(registry.find("130152", Nationality.OTHER)).isEmpty();
        assertThat(registry.find("999999", Nationality.VN)).isEmpty();
    }

    @Test
    void OTHER_국적은_넣을_수_없다() {
        PlaceConnectionFile file = new PlaceConnectionFile(List.of(new PlaceConnectionFile.Entry("1", "장소",
                Map.of(Nationality.OTHER, new PlaceConnection("어딘가", null, "닮은 점")))));

        assertThatThrownBy(() -> new PlaceConnectionRegistry(file)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void link가_비면_서버가_뜨지_않는다() {
        PlaceConnectionFile file = new PlaceConnectionFile(List.of(new PlaceConnectionFile.Entry("1", "장소",
                Map.of(Nationality.VN, new PlaceConnection("어딘가", null, " ")))));

        assertThatThrownBy(() -> new PlaceConnectionRegistry(file)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 같은_장소가_두_번_있으면_서버가_뜨지_않는다() {
        PlaceConnectionFile.Entry entry = new PlaceConnectionFile.Entry("1", "장소",
                Map.of(Nationality.VN, new PlaceConnection(null, null, "닮은 점")));

        assertThatThrownBy(() -> new PlaceConnectionRegistry(new PlaceConnectionFile(List.of(entry, entry))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 빈_사례_이름은_null로_맞춘다() {
        PlaceConnectionFile file = new PlaceConnectionFile(List.of(new PlaceConnectionFile.Entry("1", "장소",
                Map.of(Nationality.CN, new PlaceConnection("  ", "현지 이름", " 닮은 점 ")))));

        PlaceConnection connection = new PlaceConnectionRegistry(file).find("1", Nationality.CN).orElseThrow();
        assertThat(connection.counterpart()).isNull();
        assertThat(connection.localName()).isNull(); // 사례가 없으면 현지어 이름도 버린다
        assertThat(connection.link()).isEqualTo("닮은 점");
    }
}
