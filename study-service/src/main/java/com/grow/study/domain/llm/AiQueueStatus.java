package com.grow.study.domain.llm;

public enum AiQueueStatus {
    PENDING,     // 대기
    PROCESSING,  // 처리 중
    COMPLETED,   // 완료
    FAILED       // 실패 (재시도 초과)
}