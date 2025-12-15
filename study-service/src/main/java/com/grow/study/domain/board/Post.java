package com.grow.study.domain.board;

import com.grow.study.domain.AbstractEntity;
import com.grow.study.domain.study.Study;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 스터디 게시글 엔티티
 */
@Entity
@Table(
        name = "posts",
        indexes = {
                @Index(name = "idx_post_study_id", columnList = "study_id"),
                @Index(name = "idx_post_study_category", columnList = "study_id, category"),
                @Index(name = "idx_post_created_at", columnList = "createdAt DESC"),
                @Index(name = "idx_post_writer", columnList = "writerId")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id", nullable = false)
    @Comment("스터디 ID")
    private Study study;

    @Column(nullable = false)
    @Comment("작성자 회원 ID")
    private Long writerId;

    @Column(nullable = false, length = 100)
    @Comment("작성자 닉네임 (비정규화)")
    private String writerNickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Comment("카테고리")
    private PostCategory category;

    @Column(nullable = false, length = 200)
    @Comment("제목")
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    @Comment("내용")
    private String content;

    // ==================== 조회/반응 정보 ====================

    @Column(nullable = false)
    @Comment("조회수")
    private Integer viewCount = 0;

    @Column(nullable = false)
    @Comment("댓글 수")
    private Integer commentCount = 0;

    // ==================== 상태 정보 ====================

    @Column(nullable = false)
    @Comment("고정 여부")
    private Boolean pinned = false;

    @Column(nullable = false)
    @Comment("삭제 여부")
    private Boolean deleted = false;

    // ==================== 연관 관계 ====================

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<PostComment> comments = new ArrayList<>();

    // ==================== 감사(Audit) 정보 ====================

    @Column(nullable = false, updatable = false)
    @Comment("작성일시")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Comment("수정일시")
    private LocalDateTime updatedAt;

    // ==================== 생성자 ====================

    @Builder
    public Post(
            Study study,
            Long writerId,
            String writerNickname,
            PostCategory category,
            String title,
            String content
    ) {
        this.study = study;
        this.writerId = writerId;
        this.writerNickname = writerNickname;
        this.category = category;
        this.title = title;
        this.content = content;
        this.viewCount = 0;
        this.commentCount = 0;
        this.pinned = false;
        this.deleted = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // ==================== 정적 팩토리 메서드 ====================

    /**
     * 일반 게시글 생성
     */
    public static Post create(
            Study study,
            Long writerId,
            String writerNickname,
            PostCategory category,
            String title,
            String content
    ) {
        validateCategory(category, study, writerId);

        return Post.builder()
                .study(study)
                .writerId(writerId)
                .writerNickname(writerNickname)
                .category(category)
                .title(title)
                .content(content)
                .build();
    }

    /**
     * 공지 게시글 생성 (스터디장만 가능)
     */
    public static Post createNotice(
            Study study,
            Long leaderId,
            String leaderNickname,
            String title,
            String content
    ) {
        if (!study.isLeader(leaderId)) {
            throw new IllegalArgumentException("공지는 스터디장만 작성할 수 있습니다.");
        }

        return Post.builder()
                .study(study)
                .writerId(leaderId)
                .writerNickname(leaderNickname)
                .category(PostCategory.NOTICE)
                .title(title)
                .content(content)
                .build();
    }

    // ==================== 비즈니스 메서드 ====================

    /**
     * 카테고리 유효성 검증
     */
    private static void validateCategory(PostCategory category, Study study, Long writerId) {
        if (category.isLeaderOnly() && !study.isLeader(writerId)) {
            throw new IllegalArgumentException(
                    String.format("%s 카테고리는 스터디장만 작성할 수 있습니다.", category.getDescription())
            );
        }
    }

    /**
     * 게시글 수정
     */
    public void update(String title, String content) {
        validateNotDeleted();
        this.title = title;
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 카테고리 변경
     */
    public void changeCategory(PostCategory newCategory) {
        validateNotDeleted();
        if (newCategory.isLeaderOnly() && !study.isLeader(this.writerId)) {
            throw new IllegalArgumentException(
                    String.format("%s 카테고리는 스터디장만 지정할 수 있습니다.", newCategory.getDescription())
            );
        }
        this.category = newCategory;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 게시글 삭제 (soft delete)
     */
    public void delete() {
        this.deleted = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 조회수 증가
     */
    public void increaseViewCount() {
        this.viewCount++;
    }

    /**
     * 댓글 수 증가
     */
    public void increaseCommentCount() {
        this.commentCount++;
    }

    /**
     * 댓글 수 감소
     */
    public void decreaseCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount--;
        }
    }

    /**
     * 게시글 고정
     */
    public void pin() {
        validateNotDeleted();
        this.pinned = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 게시글 고정 해제
     */
    public void unpin() {
        this.pinned = false;
        this.updatedAt = LocalDateTime.now();
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
        return isWriter(memberId) || study.isLeader(memberId);
    }

    /**
     * 삭제 여부 검증
     */
    private void validateNotDeleted() {
        if (this.deleted) {
            throw new IllegalStateException("삭제된 게시글입니다.");
        }
    }

    /**
     * 공지 여부
     */
    public boolean isNotice() {
        return this.category == PostCategory.NOTICE;
    }
}
