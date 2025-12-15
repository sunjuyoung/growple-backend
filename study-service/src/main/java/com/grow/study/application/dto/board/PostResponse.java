package com.grow.study.application.dto.board;

import com.grow.study.domain.board.Post;
import com.grow.study.domain.board.PostCategory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PostResponse {

    private Long id;
    private Long studyId;
    private Long writerId;
    private String writerNickname;
    private PostCategory category;
    private String categoryName;
    private String title;
    private String content;
    private Integer viewCount;
    private Integer commentCount;
    private Boolean pinned;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isWriter;      // 현재 사용자가 작성자인지
    private Boolean canModify;     // 현재 사용자가 수정/삭제 가능한지

    public static PostResponse from(Post post) {
        return PostResponse.builder()
                .id(post.getId())
                .studyId(post.getStudy().getId())
                .writerId(post.getWriterId())
                .writerNickname(post.getWriterNickname())
                .category(post.getCategory())
                .categoryName(post.getCategory().getDescription())
                .title(post.getTitle())
                .content(post.getContent())
                .viewCount(post.getViewCount())
                .commentCount(post.getCommentCount())
                .pinned(post.getPinned())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    public static PostResponse from(Post post, Long currentMemberId) {
        return PostResponse.builder()
                .id(post.getId())
                .studyId(post.getStudy().getId())
                .writerId(post.getWriterId())
                .writerNickname(post.getWriterNickname())
                .category(post.getCategory())
                .categoryName(post.getCategory().getDescription())
                .title(post.getTitle())
                .content(post.getContent())
                .viewCount(post.getViewCount())
                .commentCount(post.getCommentCount())
                .pinned(post.getPinned())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .isWriter(post.isWriter(currentMemberId))
                .canModify(post.canModify(currentMemberId))
                .build();
    }
}
