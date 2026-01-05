package com.grow.payment.adapter.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

/**
 * 정산 배치 Job 리스너
 * Job 시작/종료 시 로깅 및 알림
 */
@Slf4j
public class SettlementJobListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("========================================");
        log.info("정산 배치 Job 시작");
        log.info("Job Name: {}", jobExecution.getJobInstance().getJobName());
        log.info("Job Parameters: {}", jobExecution.getJobParameters());
        log.info("Start Time: {}", jobExecution.getStartTime());
        log.info("========================================");
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("========================================");
        log.info("정산 배치 Job 종료");
        log.info("Job Name: {}", jobExecution.getJobInstance().getJobName());
        log.info("Status: {}", jobExecution.getStatus());
        log.info("Start Time: {}", jobExecution.getStartTime());
        log.info("End Time: {}", jobExecution.getEndTime());
        
        if (jobExecution.getStatus().isUnsuccessful()) {
            log.error("Job 실패! 예외 목록:");
            jobExecution.getAllFailureExceptions().forEach(ex -> 
                log.error("  - {}", ex.getMessage())
            );
            // TODO: Slack 알림 발송
        } else {
            log.info("Job 성공적으로 완료");
        }
        
        log.info("========================================");
    }
}
