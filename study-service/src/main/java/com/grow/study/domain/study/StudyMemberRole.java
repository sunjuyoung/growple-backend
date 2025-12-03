package com.grow.study.domain.study;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StudyMemberRole {
    LEADER("스터디장", "스터디를 개설하고 관리하는 역할"),
    MEMBER("참가자", "스터디에 참가한 일반 멤버");

    private final String displayName;
    private final String description;
}
