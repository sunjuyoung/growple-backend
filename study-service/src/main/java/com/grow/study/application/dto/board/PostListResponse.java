package com.grow.study.application.dto.board;

import com.grow.study.domain.board.Post;
import com.grow.study.domain.board.PostCategory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 게시글 목록 조회용 DTO (내용 제외)
 */
@Getter
@Builder
public class PostListResponse {

    private Long id;
    private Long writerId;
    private String writerNickname;
    private PostCategory category;
    private String categoryName;
    private String title;
    private Integer viewCount;
    private Integer commentCount;
    private Boolean pinned;
    private LocalDateTime createdAt;

    public static PostListResponse from(Post post) {
        return PostListResponse.builder()
                .id(post.getId())
                .writerId(post.getWriterId())
                .writerNickname(post.getWriterNickname())
                .category(post.getCategory())
                .categoryName(post.getCategory().getDescription())
                .title(post.getTitle())
                .viewCount(post.getViewCount())
                .commentCount(post.getCommentCount())
                .pinned(post.getPinned())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
