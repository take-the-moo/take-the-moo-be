package com.hackathon.domain.place.service;

import com.hackathon.domain.place.entity.Station;
import com.hackathon.domain.place.exception.StationErrorType;
import com.hackathon.domain.place.exception.StationException;
import com.hackathon.domain.place.repository.StationRepository;
import com.hackathon.global.config.AppProperties;
import com.hackathon.global.util.GeoUtils;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 지하철역 조회와 기준 위치 정하기 (F2 추천·F4 주변 장소).
 * 위치(lat/lng)는 계산에만 쓰고 저장하거나 로그·예외 detail에 남기지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StationService {

    /**
     * 역 테이블에 기본 역이 없을 때(T Data 키 없이 새 DB로 띄운 경우 등) 쓰는 독립문역 3호선 좌표.
     * T Data 값과 같다. 기본 역 설정을 바꾸면 이 값은 쓰이지 않을 수 있다
     */
    static final double FALLBACK_LATITUDE = 37.574571;
    static final double FALLBACK_LONGITUDE = 126.957748;

    private final StationRepository stationRepository;
    private final AppProperties properties;

    /**
     * 기준 위치를 정한다. 좌표 → 고른 역 → 기본 위치(app.demo 좌표, 없으면 기본 역) 순서.
     * - lat·lng는 둘 다 있어야 한다. 하나만 오거나 범위를 벗어나면 422 STATION_INVALID_LOCATION
     * - 없는 stationId면 404 STATION_NOT_FOUND
     */
    public BaseLocation resolve(Double lat, Double lng, Long stationId) {
        if (lat != null || lng != null) {
            if (lat == null || lng == null || Math.abs(lat) > 90 || Math.abs(lng) > 180) {
                throw new StationException(StationErrorType.INVALID_LOCATION, "lat·lng 중 하나가 없거나 범위 밖");
            }
            return new BaseLocation(lat, lng, nearestStationName(lat, lng), BaseLocation.Source.COORDINATES);
        }
        if (stationId != null) {
            Station station = stationRepository.findById(stationId)
                    .orElseThrow(() -> new StationException(StationErrorType.NOT_FOUND, "stationId=" + stationId));
            return new BaseLocation(station.getLatitude(), station.getLongitude(), station.getName(),
                    BaseLocation.Source.STATION);
        }
        return defaultLocation();
    }

    /**
     * "역 선택" 목록 (위치 거부·데모용). app.station.selectable-names 순서대로, 거리로 거르지 않는다.
     * 같은 이름의 호선은 모두 넣는다 (신촌 2호선·경의중앙선은 위치가 다르다). 같은 이름 안에서는 역 코드 순.
     * DB에 없는 이름은 건너뛴다 (역 동기화 전이면 빈 목록)
     */
    public List<Station> getSelectableStations() {
        return properties.station().selectableNames().stream()
                .flatMap(name -> stationRepository.findAllByName(name).stream()
                        .sorted(Comparator.comparing(Station::getStationCode)))
                .toList();
    }

    /** 기본 역(app.demo.default-station). 호선이 여럿이면 역 코드가 가장 작은 행. 역 테이블에 없으면 empty */
    public Optional<Station> findDefaultStation() {
        return stationRepository.findAllByName(properties.demo().defaultStation()).stream()
                .min(Comparator.comparing(Station::getStationCode));
    }

    private BaseLocation defaultLocation() {
        AppProperties.Demo demo = properties.demo();
        if (demo.defaultLatitude() != null && demo.defaultLongitude() != null) {
            return new BaseLocation(demo.defaultLatitude(), demo.defaultLongitude(), demo.defaultName(),
                    BaseLocation.Source.DEFAULT);
        }
        String name = demo.defaultStation();
        return findDefaultStation()
                .map(station -> new BaseLocation(station.getLatitude(), station.getLongitude(), name,
                        BaseLocation.Source.DEFAULT))
                .orElseGet(() -> {
                    log.warn("기본 역({})이 역 테이블에 없어 고정 좌표를 쓴다 (역 동기화 전이거나 이름이 다르다)", name);
                    return new BaseLocation(FALLBACK_LATITUDE, FALLBACK_LONGITUDE, name, BaseLocation.Source.DEFAULT);
                });
    }

    private String nearestStationName(double lat, double lng) {
        return stationRepository.findAll().stream()
                .min(Comparator.comparingInt(s -> GeoUtils.distanceMeters(lat, lng, s.getLatitude(), s.getLongitude())))
                .map(Station::getName)
                .orElse(null);
    }
}
