package com.grow.payment.adapter.config;

import org.springframework.context.annotation.Configuration;

/**
 * Spring Batch 설정
 *
 * Spring Boot 3.x에서는 @EnableBatchProcessing 없이도 자동 구성이 활성화됩니다.
 * JpaTransactionManager가 자동으로 transactionManager로 등록됩니다.
 */
@Configuration
public class BatchConfig {

}
