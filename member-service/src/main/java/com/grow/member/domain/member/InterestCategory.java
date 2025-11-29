package com.grow.member.domain.member;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InterestCategory {
    DEVELOPMENT("개발", "💻"),
    LANGUAGE("외국어", "🌍"),
    CERTIFICATION("자격증", "📜"),
    HOBBY("취미", "🎨"),
    OTHER("기타", "📚");

    private final String title;
    private final String icon;
}