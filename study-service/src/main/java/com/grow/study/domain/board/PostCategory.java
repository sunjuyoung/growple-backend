package com.grow.study.domain.board;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 게시글 카테고리
 */
@Getter
@RequiredArgsConstructor
public enum PostCategory {

    NOTICE("공지", true),      // 스터디장만 작성 가능
    QUESTION("질문", false),
    INFO("정보", false);

    private final String description;
    private final boolean leaderOnly;  // 스터디장만 작성 가능 여부

    public boolean isLeaderOnly() {
        return leaderOnly;
    }
}
