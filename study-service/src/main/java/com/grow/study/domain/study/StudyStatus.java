package com.grow.study.domain.study;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StudyStatus {

    PENDING("대기 중", "스터디가 개설 결제 대기 중인 상태"),
    RECRUITING("모집 중", "참가자를 모집하고 있는 상태"),
    RECRUIT_CLOSED("모집마감", "참가자 모집마감"),
    IN_PROGRESS("진행 중", "스터디가 시작되어 진행 중인 상태"),
    COMPLETED("완료", "스터디가 정상적으로 종료된 상태"),
    CANCELLED("취소", "최소 인원 미달 등의 이유로 취소된 상태"),
    SETTLED("정산 완료", "정산 완료");

    private final String displayName;
    private final String description;

    public boolean isActive() {
        return this == RECRUITING || this == IN_PROGRESS;
    }

    public boolean isFinished() {
        return this == COMPLETED || this == CANCELLED || this == SETTLED;
    }
}
