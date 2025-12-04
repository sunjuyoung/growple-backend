package com.grow.study.domain.study;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StudyVisibility {
    PUBLIC("공개", "누구나 검색하고 참여 가능"),
    PRIVATE("비공개", "링크를 가진 사람만 참여 가능");

    private final String displayName;
    private final String description;
}
