package com.hackathon.domain.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hackathon.domain.place.entity.Station;
import com.hackathon.domain.place.exception.StationErrorType;
import com.hackathon.domain.place.exception.StationException;
import com.hackathon.domain.place.repository.StationRepository;
import com.hackathon.global.config.AppProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * H2는 테스트 컨텍스트끼리 공유되므로 역 코드는 T로 시작하는 테스트 전용 값을 쓰고,
 * 좌표는 서울 밖(제주)으로 두어 다른 테스트의 역과 섞이지 않게 한다.
 */
@SpringBootTest(properties = "llm.mock=true")
class StationServiceTest {

    @Autowired
    StationService stationService;

    @Autowired
    StationRepository stationRepository;

    @Autowired
    AppProperties properties;

    private Station saveStation(String code, String name, String line, double lat, double lng) {
        Station station = Station.of(code);
        station.update(name, line, lat, lng);
        return stationRepository.save(station);
    }

    @Test
    void 좌표가_있으면_좌표를_쓰고_가장_가까운_역을_기준_역으로_둔다() {
        saveStation("T9001", "제주가까운역", "테스트선", 33.5000, 126.5000);
        saveStation("T9002", "제주먼역", "테스트선", 33.5300, 126.5300);

        BaseLocation location = stationService.resolve(33.5010, 126.5010, 999L);

        assertThat(location.source()).isEqualTo(BaseLocation.Source.COORDINATES);
        assertThat(location.latitude()).isEqualTo(33.5010);
        assertThat(location.longitude()).isEqualTo(126.5010);
        assertThat(location.baseStation()).isEqualTo("제주가까운역");
    }

    @Test
    void 좌표가_없으면_고른_역의_위치를_쓴다() {
        Station station = saveStation("T9003", "제주선택역", "테스트선", 33.4000, 126.4000);

        BaseLocation location = stationService.resolve(null, null, station.getId());

        assertThat(location.source()).isEqualTo(BaseLocation.Source.STATION);
        assertThat(location.latitude()).isEqualTo(33.4000);
        assertThat(location.baseStation()).isEqualTo("제주선택역");
    }

    @Test
    void 아무것도_없으면_기본_위치_명지대_인문캠퍼스() {
        BaseLocation location = stationService.resolve(null, null, null);

        assertThat(location.source()).isEqualTo(BaseLocation.Source.DEFAULT);
        assertThat(location.baseStation()).isEqualTo("명지대 인문캠퍼스");
        assertThat(location.latitude()).isEqualTo(37.5803);
        assertThat(location.longitude()).isEqualTo(126.9233);
    }

    @Test
    void 기본_좌표를_비우면_기본_역() {
        Station station = saveStation("T9004", "독립문", "3호선", 37.574571, 126.957748);
        StationRepository repository = mock(StationRepository.class);
        when(repository.findAllByName("독립문")).thenReturn(List.of(station));

        BaseLocation location = new StationService(repository, withoutDefaultCoordinates()).resolve(null, null, null);

        assertThat(location.source()).isEqualTo(BaseLocation.Source.DEFAULT);
        assertThat(location.baseStation()).isEqualTo("독립문");
        assertThat(location.latitude()).isEqualTo(37.574571);
    }

    @Test
    void 기본_역이_역_테이블에_없으면_고정_좌표를_쓴다() {
        StationRepository emptyRepository = mock(StationRepository.class);
        when(emptyRepository.findAllByName("독립문")).thenReturn(List.of());

        BaseLocation location = new StationService(emptyRepository, withoutDefaultCoordinates()).resolve(null, null, null);

        assertThat(location.source()).isEqualTo(BaseLocation.Source.DEFAULT);
        assertThat(location.baseStation()).isEqualTo("독립문");
        assertThat(location.latitude()).isEqualTo(StationService.FALLBACK_LATITUDE);
        assertThat(location.longitude()).isEqualTo(StationService.FALLBACK_LONGITUDE);
    }

    /** app.demo.default-latitude·longitude를 비운 설정 (기본 역으로 돌아간다) */
    private AppProperties withoutDefaultCoordinates() {
        AppProperties.Demo demo = new AppProperties.Demo(properties.demo().defaultStation(), null, null, null,
                properties.demo().password());
        return new AppProperties(properties.storage(), properties.weather(), properties.tour(), properties.station(),
                demo, properties.visit(), properties.auth());
    }

    @Test
    void 없는_역이면_404() {
        assertThatThrownBy(() -> stationService.resolve(null, null, -1L))
                .isInstanceOf(StationException.class)
                .extracting("errorCode").isEqualTo(StationErrorType.NOT_FOUND);
    }

    @Test
    void 위도나_경도_하나만_오거나_범위를_벗어나면_422() {
        assertThatThrownBy(() -> stationService.resolve(37.5, null, null))
                .isInstanceOf(StationException.class)
                .extracting("errorCode").isEqualTo(StationErrorType.INVALID_LOCATION);
        assertThatThrownBy(() -> stationService.resolve(null, 126.9, null))
                .isInstanceOf(StationException.class);
        assertThatThrownBy(() -> stationService.resolve(91.0, 126.9, null))
                .isInstanceOf(StationException.class);
    }
}
