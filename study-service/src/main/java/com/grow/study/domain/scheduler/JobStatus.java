package com.grow.study.domain.scheduler;

public enum JobStatus {
    PENDING("대기 중"),
    PROCESSING("처리 중"),
    COMPLETED("완료"),
    FAILED("실패");

    private final String description;

    JobStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isClaimable() {
        return this == PENDING || this == FAILED;
    }
}
