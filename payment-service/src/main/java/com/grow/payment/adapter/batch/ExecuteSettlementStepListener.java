package com.grow.payment.adapter.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

/**
 * Step 2 Listener: 정산 실행 Step 모니터링
 */
@Slf4j
public class ExecuteSettlementStepListener implements StepExecutionListener {

    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info("========================================");
        log.info("[정산 실행 Step 시작] jobExecutionId: {}, stepName: {}",
                stepExecution.getJobExecutionId(),
                stepExecution.getStepName());
        log.info("========================================");
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        log.info("========================================");
        log.info("[정산 실행 Step 완료]");
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
