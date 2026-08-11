package com.project.back.domain.demo.scheduler;

import com.project.back.domain.demo.service.DemoDataResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "demo.enabled", havingValue = "true")
public class DemoDataResetScheduler {

    private final DemoDataResetService demoDataResetService;

    // 매일 자정 실행 — 공개 데모 계정(demo.staff / demo.manager)이 만든 견적·고객 데이터를 초기화한다.
    @Scheduled(cron = "${demo.reset.cron:0 0 0 * * *}")
    public void resetDemoData() {
        log.info("데모 데이터 초기화 스케줄러 시작");
        demoDataResetService.resetDemoData();
    }
}
