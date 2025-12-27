package com.grow.study.adapter.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 비동기 처리 설정
 * 벡터 Document 생성을 위한 비동기 이벤트 처리
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
