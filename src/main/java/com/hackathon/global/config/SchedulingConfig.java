package com.hackathon.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @Scheduled를 켠다. 스케줄 작업은 cron에 zone = "Asia/Seoul"을 지정하고,
 * 켜고 끄는 설정(예: app.tour.sync-enabled)을 메서드 안에서 확인한다.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class SchedulingConfig {
}
