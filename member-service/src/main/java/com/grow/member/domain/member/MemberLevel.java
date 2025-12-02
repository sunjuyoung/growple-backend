package com.grow.member.domain.member;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberLevel {
    NEWCOMER("새내기", 0, 99, 1),
    PASSIONATE("열정러", 100, 299, 2),
    EXPERT("고수", 300, 599, 3),
    MASTER("마스터", 600, Integer.MAX_VALUE, 4);

    private final String title;
    private final int minScore;
    private final int maxScore;
    private final int level;

    /**
     * 활동 점수에 따른 레벨 반환
     */
    public static MemberLevel fromScore(int score) {
        for (MemberLevel level : values()) {
            if (score >= level.minScore && score <= level.maxScore) {
                return level;
            }
        }
        return NEWCOMER;
    }

    /**
     * 다음 레벨까지 필요한 점수 계산
     */
    public int getScoreToNextLevel(int currentScore) {
        if (this == MASTER) {
            return 0; // 최고 레벨
        }
        return this.maxScore + 1 - currentScore;
    }
}