package com.grow.payment.adapter.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 정산 배치 Job 설정
 * 
 * Job 구조:
 * ┌─────────────────────────────────────────────────────────────┐
 * │                    settlementJob                            │
 * ├─────────────────────────────────────────────────────────────┤
 * │  Step 1: settlementCreationStep                             │
 * │  - Study Service에서 COMPLETED 스터디 조회                    │
 * │  - Settlement + SettlementItem 생성                         │
 * ├─────────────────────────────────────────────────────────────┤
 * │  Step 2: settlementExecutionStep                            │
 * │  - PENDING 상태의 Settlement 처리                            │
 * │  - 각 SettlementItem에 대해 포인트 지급                       │
 * │  - 완료 시 Settlement 상태를 COMPLETED로 변경                 │
 * └─────────────────────────────────────────────────────────────┘
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SettlementJobConfig {

    private final JobRepository jobRepository;

    @Bean
    public Job settlementJob(
            Step createSettlementStep,
            Step executeSettlementStep
    ) {
        return new JobBuilder("settlementJob", jobRepository)
                .incrementer(new RunIdIncrementer())  // 동일 파라미터로 재실행 허용
                .start(createSettlementStep)          // Step 1: 정산 대상 생성
                .next(executeSettlementStep)          // Step 2: 정산 실행
                .listener(new SettlementJobListener())
                .build();
    }
}