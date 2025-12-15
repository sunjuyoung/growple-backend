package com.grow.study.application.dto.board;

import com.grow.study.domain.board.PostComment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CommentResponse {

    private Long id;
    private Long postId;
    private Long writerId;
    private String writerNickname;
    private String content;
    private Boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isWriter;      // 현재 사용자가 작성자인지
    private Boolean canModify;     // 현재 사용자가 수정/삭제 가능한지

    public static CommentResponse from(PostComment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .writerId(comment.getWriterId())
                .writerNickname(comment.getWriterNickname())
                .content(comment.getDisplayContent())
                .deleted(comment.getDeleted())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    public static CommentResponse from(PostComment comment, Long currentMemberId) {
        return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .writerId(comment.getWriterId())
                .writerNickname(comment.getWriterNickname())
                .content(comment.getDisplayContent())
                .deleted(comment.getDeleted())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .isWriter(comment.isWriter(currentMemberId))
                .canModify(comment.canModify(currentMemberId))
                .build();
    }
}
