package com.grow.study.domain.scheduler;

public enum JobType {
    RECRUITMENT_DEADLINE("모집 마감 처리"),
    STUDY_START("스터디 시작 처리"),
    STUDY_COMPLETION("스터디 종료 처리");

    private final String description;

    JobType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
