package com.hackathon.domain.recommend.service;

import com.hackathon.domain.place.entity.Station;
import com.hackathon.domain.place.service.StationService;
import com.hackathon.domain.recommend.dto.response.StationResponseDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * F2 지금 추천. 장소·역 데이터는 domain/place의 서비스로만 읽는다. 추천 결과는 저장하지 않는다.
 */
@Service
@RequiredArgsConstructor
public class RecommendService {

    private final StationService stationService;

    /** 역 선택 목록. 기본 역(GET /api/now가 위치 없이 쓰는 역)에 isDefault를 붙인다 */
    public List<StationResponseDto> getStations() {
        Long defaultId = stationService.findDefaultStation().map(Station::getId).orElse(null);
        return stationService.getSelectableStations().stream()
                .map(station -> StationResponseDto.of(station, station.getId().equals(defaultId)))
                .toList();
    }
}
