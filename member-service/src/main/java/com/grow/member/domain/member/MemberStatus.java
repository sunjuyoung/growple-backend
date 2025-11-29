package com.grow.member.domain.member;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberStatus {
    ACTIVE("활성", "정상 활동 가능한 회원"),
    SUSPENDED("정지", "활동이 정지된 회원"),
    WITHDRAWN("탈퇴", "탈퇴한 회원");

    private final String title;
    private final String description;

    public boolean isActive() {
        return this == MemberStatus.ACTIVE;
    }
}
