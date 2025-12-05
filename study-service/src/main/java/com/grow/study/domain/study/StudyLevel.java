package com.grow.study.domain.study;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StudyLevel {
    BEGINNER("입문", "처음 시작하는 분들도 OK"),
    BASIC("초급", "기초 지식이 있는 분"),
    INTERMEDIATE("중급", "어느 정도 경험이 있는 분"),
    ADVANCED("고급", "심화 학습을 원하는 분");

    private final String displayName;
    private final String description;

    public static StudyLevel fromString(String value) {
        for (StudyLevel level : values()) {
            if (level.name().equalsIgnoreCase(value)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Invalid level: " + value);
    }

    public static StudyLevel fromDisplayName(String displayName) {
        for (StudyLevel level : values()) {
            if (level.displayName.equals(displayName)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Invalid level displayName: " + displayName);
    }
}
