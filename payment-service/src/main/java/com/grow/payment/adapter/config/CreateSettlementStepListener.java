package com.grow.payment.adapter.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

/**
 * Step 1 Listener: 정산 생성 Step 모니터링
 *
 * - Step 시작/종료 시 로깅
 * - 처리 건수 및 스킵 건수 기록
 */
@Slf4j
public class CreateSettlementStepListener implements StepExecutionListener {

    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info("========================================");
        log.info("[정산 생성 Step 시작] jobExecutionId: {}, stepName: {}",
                stepExecution.getJobExecutionId(),
                stepExecution.getStepName());
        log.info("========================================");
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        log.info("========================================");
        log.info("[정산 생성 Step 완료]");
        log.info("  - Status: {}", stepExecution.getStatus());
        log.info("  - Read Count: {}", stepExecution.getReadCount());
        log.info("  - Write Count: {}", stepExecution.getWriteCount());
        log.info("  - Filter Count: {}", stepExecution.getFilterCount());
        log.info("  - Skip Count: {}", stepExecution.getSkipCount());
        log.info("  - Commit Count: {}", stepExecution.getCommitCount());

        if (stepExecution.getFailureExceptions() != null
                && !stepExecution.getFailureExceptions().isEmpty()) {
            log.error("  - Failures:");
            for (Throwable t : stepExecution.getFailureExceptions()) {
                log.error("    > {}: {}", t.getClass().getSimpleName(), t.getMessage());
            }
        }

        log.info("========================================");

        return stepExecution.getExitStatus();
    }
}
