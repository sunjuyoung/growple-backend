package com.grow.payment.adapter.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 정산 배치 스케줄러
 *
 * 매일 새벽 3시에 정산 배치 실행
 * - Step 1: 완료된 스터디에 대한 Settlement 생성
 * - Step 2: PENDING 상태의 Settlement 정산 실행 (포인트 지급)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job settlementJob;

    /**
     * 매일 새벽 3시에 정산 배치 실행
     * cron: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void runSettlementJob() {
        log.info("정산 배치 스케줄 실행 시작 - {}", LocalDateTime.now());

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("executedAt", LocalDateTime.now())
                    .toJobParameters();

            jobLauncher.run(settlementJob, jobParameters);

            log.info("정산 배치 스케줄 실행 완료");
        } catch (Exception e) {
            log.error("정산 배치 스케줄 실행 실패", e);
        }
    }
}
