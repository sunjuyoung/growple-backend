package com.grow.payment.adapter.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Step 1: 정산 대상 생성
 *
 * - Study Service에서 COMPLETED 스터디 조회
 * - Settlement + SettlementItem 생성
 * - DB 저장
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class CreateSettlementStepConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Step createSettlementStep(
            ExpiredStudyReader expiredStudyReader,
            CreateSettlementProcessor createSettlementProcessor,
            SettlementWriter settlementWriter
    ) {
        return new StepBuilder("createSettlementStep", jobRepository)
                .<ExpiredStudyDto, SettlementCreationResult>chunk(10, transactionManager)
                .reader(expiredStudyReader)
                .processor(createSettlementProcessor)
                .writer(settlementWriter)
                .faultTolerant()
                .skipLimit(5)
                .skip(Exception.class)
                .listener(new CreateSettlementStepListener())
                .build();
    }
}