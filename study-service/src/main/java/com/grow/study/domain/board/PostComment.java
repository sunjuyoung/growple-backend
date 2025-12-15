package com.grow.study.domain.board;

import com.grow.study.domain.AbstractEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 게시글 댓글 엔티티
 */
@Entity
@Table(
        name = "post_comments",
        indexes = {
                @Index(name = "idx_comment_post_id", columnList = "post_id"),
                @Index(name = "idx_comment_writer", columnList = "writerId"),
                @Index(name = "idx_comment_created_at", columnList = "createdAt ASC")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostComment extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    @Comment("게시글 ID")
    private Post post;

    @Column(nullable = false)
    @Comment("작성자 회원 ID")
    private Long writerId;

    @Column(nullable = false, length = 100)
    @Comment("작성자 닉네임 (비정규화)")
    private String writerNickname;

    @Column(nullable = false, length = 1000)
    @Comment("댓글 내용")
    private String content;

    // ==================== 상태 정보 ====================

    @Column(nullable = false)
    @Comment("삭제 여부")
    private Boolean deleted = false;

    // ==================== 감사(Audit) 정보 ====================

    @Column(nullable = false, updatable = false)
    @Comment("작성일시")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Comment("수정일시")
    private LocalDateTime updatedAt;

    // ==================== 생성자 ====================

    @Builder
    public PostComment(
            Post post,
            Long writerId,
            String writerNickname,
            String content
    ) {
        this.post = post;
        this.writerId = writerId;
        this.writerNickname = writerNickname;
        this.content = content;
        this.deleted = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // ==================== 정적 팩토리 메서드 ====================

    /**
     * 댓글 생성
     */
    public static PostComment create(
            Post post,
            Long writerId,
            String writerNickname,
            String content
    ) {
        validateContent(content);

        PostComment comment = PostComment.builder()
                .post(post)
                .writerId(writerId)
                .writerNickname(writerNickname)
                .content(content)
                .build();

        post.increaseCommentCount();
        return comment;
    }

    // ==================== 비즈니스 메서드 ====================

    /**
     * 내용 유효성 검증
     */
    private static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("댓글 내용은 필수입니다.");
        }
        if (content.length() > 1000) {
            throw new IllegalArgumentException("댓글은 1000자를 초과할 수 없습니다.");
        }
    }

    /**
     * 댓글 수정
     */
    public void update(String content) {
        validateNotDeleted();
        validateContent(content);
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 댓글 삭제 (soft delete)
     */
    public void delete() {
        validateNotDeleted();
        this.deleted = true;
        this.updatedAt = LocalDateTime.now();
        this.post.decreaseCommentCount();
    }

    /**
     * 작성자 확인
     */
    public boolean isWriter(Long memberId) {
        return this.writerId.equals(memberId);
    }

    /**
     * 수정/삭제 가능 여부 (작성자 또는 스터디장)
     */
    public boolean canModify(Long memberId) {
        return isWriter(memberId) || post.getStudy().isLeader(memberId);
    }

    /**
     * 삭제 여부 검증
     */
    private void validateNotDeleted() {
        if (this.deleted) {
            throw new IllegalStateException("삭제된 댓글입니다.");
        }
    }

    /**
     * 삭제 여부
     */
    public boolean isDeleted() {
        return this.deleted;
    }

    /**
     * 표시용 내용 (삭제된 경우 대체 텍스트)
     */
    public String getDisplayContent() {
        return this.deleted ? "삭제된 댓글입니다." : this.content;
    }
}
