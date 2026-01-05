package com.grow.payment.adapter.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Spring Batch 설정
 * 
 * Spring Boot 3.x에서는 @EnableBatchProcessing 사용 시 자동 설정이 비활성화됩니다.
 * 따라서 명시적으로 JobRepository와 TransactionManager를 설정합니다.
 */
@Configuration
@EnableBatchProcessing
public class BatchConfig {

    /**
     * 배치 트랜잭션 매니저
     * 배치 메타데이터 관리에 사용
     */
    @Bean
    public PlatformTransactionManager batchTransactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
