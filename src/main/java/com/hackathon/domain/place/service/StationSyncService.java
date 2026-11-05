package com.hackathon.domain.place.service;

import com.hackathon.domain.place.client.StationApiClient;
import com.hackathon.domain.place.client.StationItem;
import com.hackathon.domain.place.entity.Station;
import com.hackathon.domain.place.repository.StationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T Data 지하철역을 outStnNum 기준으로 upsert한다. 응답에 없는 역은 지우지 않는다 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StationSyncService {

    private final StationApiClient client;
    private final StationRepository stationRepository;

    public boolean isConfigured() {
        return client.isConfigured();
    }

    /** @return 저장한 역 수 */
    @Transactional
    public int sync() {
        List<StationItem> items = client.fetchAll();
        int saved = 0;
        for (StationItem item : items) {
            if (!item.isValid()) continue;
            Station station = stationRepository.findByStationCode(item.outStnNum())
                    .orElseGet(() -> Station.of(item.outStnNum()));
            station.update(item.stnKrNm(), item.lineNm(),
                    Double.parseDouble(item.convY()), Double.parseDouble(item.convX()));
            stationRepository.save(station);
            saved++;
        }
        log.info("지하철역 동기화 완료: 받은 {}건, 저장 {}건", items.size(), saved);
        return saved;
    }
}
