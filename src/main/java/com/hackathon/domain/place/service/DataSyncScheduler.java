package com.hackathon.domain.place.service;

import com.hackathon.domain.place.repository.StationRepository;
import com.hackathon.global.config.AppProperties;
import com.hackathon.global.external.ExternalApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 장소(TourAPI)·지하철역(T Data) 동기화 실행 시점.
 * - 서버 시작: 장소는 매번(보강 파일을 고치고 재시작하면 바로 반영), 역은 테이블이 비어 있을 때만
 *   (가상 스레드에서 돌려 부팅을 막지 않는다)
 * - 매일 app.tour.sync-cron (기본 04:00): 둘 다
 * 키가 없거나 sync-enabled=false면 건너뛰고, 실패해도 기존 데이터를 유지한다. 관리자 API는 두지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSyncScheduler {

    private final PlaceSyncService placeSyncService;
    private final StationSyncService stationSyncService;
    private final StationRepository stationRepository;
    private final AppProperties properties;

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        boolean stations = stationRepository.count() == 0;
        Thread.ofVirtual().name("startup-data-sync").start(() -> {
            if (stations) syncStations();
            syncPlaces();
        });
    }

    @Scheduled(cron = "${app.tour.sync-cron:0 0 4 * * *}", zone = "Asia/Seoul")
    public void syncDaily() {
        syncStations();
        syncPlaces();
    }

    private void syncStations() {
        if (!properties.station().syncEnabled()) return;
        if (!stationSyncService.isConfigured()) {
            log.info("지하철역 동기화 건너뜀: app.station.api-key 없음");
            return;
        }
        run("지하철역", stationSyncService::sync);
    }

    private void syncPlaces() {
        if (!properties.tour().syncEnabled()) return;
        if (!placeSyncService.isConfigured()) {
            log.info("장소 동기화 건너뜀: app.tour.service-key 없음");
            return;
        }
        run("장소", placeSyncService::sync);
    }

    private void run(String name, Runnable task) {
        try {
            task.run();
        } catch (ExternalApiException e) {
            log.warn("{} 동기화 실패, 기존 데이터 유지: {}", name, e.getDetail());
        } catch (RuntimeException e) {
            // 인증키가 든 URL이 메시지에 섞일 수 있어 예외 종류만 남긴다
            log.error("{} 동기화 중 예상치 못한 오류, 기존 데이터 유지: {}", name, e.getClass().getName());
        }
    }
}
