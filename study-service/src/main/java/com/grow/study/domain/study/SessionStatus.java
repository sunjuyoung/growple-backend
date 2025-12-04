package com.grow.study.domain.study;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SessionStatus {
    SCHEDULED("예정", "아직 진행되지 않은 세션"),
    IN_PROGRESS("진행 중", "현재 진행 중인 세션"),
    COMPLETED("완료", "정상적으로 완료된 세션"),
    CANCELLED("취소", "취소된 세션");

    private final String displayName;
    private final String description;

    public boolean isActive() {
        return this == SCHEDULED || this == IN_PROGRESS;
    }
}
