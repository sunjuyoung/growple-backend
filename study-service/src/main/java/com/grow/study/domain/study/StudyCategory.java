package com.grow.study.domain.study;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StudyCategory {
    DEVELOPMENT("개발", "💻"),
    LANGUAGE("외국어", "🌍"),
    CERTIFICATE("자격증", "📜"),
    HOBBY("취미", "🎨"),
    ETC("기타", "📚");

    private final String displayName;
    private final String emoji;

    public static StudyCategory fromString(String value) {
        for (StudyCategory category : values()) {
            if (category.name().equalsIgnoreCase(value)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Invalid category: " + value);
    }
}
