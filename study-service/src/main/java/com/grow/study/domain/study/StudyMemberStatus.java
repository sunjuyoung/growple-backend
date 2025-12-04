package com.grow.study.domain.study;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StudyMemberStatus {
    ACTIVE("활동 중", "정상적으로 스터디에 참여 중인 상태"),
    WITHDRAWN("탈퇴", "중도 탈퇴한 상태"),
    EXPELLED("강제 퇴장", "스터디장에 의해 강제 퇴장된 상태");

    private final String displayName;
    private final String description;

    public boolean isActive() {
        return this == ACTIVE;
    }
}
