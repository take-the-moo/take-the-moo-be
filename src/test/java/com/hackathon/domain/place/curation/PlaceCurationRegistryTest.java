package com.hackathon.domain.place.curation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** 실제 data/place-curation.json이 읽히는지 (형식이 깨지면 서버가 안 뜨므로 테스트에서 먼저 잡는다) */
class PlaceCurationRegistryTest {

    @Test
    void 보강_파일을_읽는다() throws Exception {
        PlaceCurationRegistry registry = new PlaceCurationRegistry(JsonMapper.builder().build());

        assertThat(registry.find("130152")).get()
                .extracting(PlaceCuration::sensitiveHistory).isEqualTo(true); // 서대문형무소역사관
        assertThat(registry.isExcluded("3078407")).isTrue(); // 금호아트홀 연세
        assertThat(registry.isExcluded("130152")).isFalse();
    }
}
