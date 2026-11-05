package com.hackathon.domain.recommend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hackathon.domain.place.entity.Station;
import com.hackathon.domain.place.repository.StationRepository;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * H2는 테스트 컨텍스트끼리 공유되어 다른 테스트가 넣은 역이 섞일 수 있다.
 * 그래서 개수를 단정하지 않고 이 테스트가 넣은 역의 순서·호선·기본 역 표시를 본다.
 */
@SpringBootTest(properties = "llm.mock=true")
@AutoConfigureMockMvc
class StationListTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    StationRepository stationRepository;

    private void saveStation(String code, String name, String line, double lat, double lng) {
        Station station = Station.of(code);
        station.update(name, line, lat, lng);
        stationRepository.save(station);
    }

    @Test
    void 설정한_순서대로_같은_이름의_호선을_모두_주고_기본_역을_표시한다() throws Exception {
        // 일부러 설정 순서와 다르게 넣는다
        saveStation("T8005", "가좌", "경의중앙선", 37.568, 126.915);
        saveStation("T8004", "신촌", "경의중앙선", 37.559, 126.942);
        saveStation("T8003", "신촌", "2호선", 37.555, 126.936);
        saveStation("T8002", "서대문", "5호선", 37.566, 126.966);
        saveStation("T8001", "독립문", "3호선", 37.574571, 126.957748);
        saveStation("T8006", "시청", "1호선", 37.565, 126.977);

        String body = mvc.perform(get("/api/stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultType").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].stationId").isNumber())
                .andExpect(jsonPath("$.data[0].isDefault").isBoolean())
                .andReturn().getResponse().getContentAsString();

        List<Map<String, Object>> stations = JsonPath.read(body, "$.data");
        List<String> mine = stations.stream()
                .filter(s -> List.of("독립문", "서대문", "신촌", "가좌", "시청").contains(s.get("name")))
                .map(s -> s.get("name") + "/" + s.get("line"))
                .distinct()
                .toList();

        assertThat(mine).as("설정 순서(독립문 → 서대문 → 신촌 → 가좌), 신촌은 호선 둘 다, 설정에 없는 시청은 빠진다")
                .containsSubsequence("독립문/3호선", "서대문/5호선", "신촌/2호선", "신촌/경의중앙선", "가좌/경의중앙선")
                .doesNotContain("시청/1호선");

        List<Map<String, Object>> defaults = stations.stream().filter(s -> Boolean.TRUE.equals(s.get("isDefault"))).toList();
        assertThat(defaults).as("기본 역은 하나").hasSize(1);
        assertThat(defaults.getFirst().get("name")).isEqualTo("독립문");
    }
}
