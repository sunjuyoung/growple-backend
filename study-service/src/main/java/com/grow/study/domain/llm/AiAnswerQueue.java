package com.grow.study.domain.llm;

import com.grow.study.domain.board.Post;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "ai_answer_queue",
        indexes = {
                @Index(name = "idx_ai_queue_status_created", columnList = "status, createdAt"),
                @Index(name = "idx_ai_queue_post", columnList = "post_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiAnswerQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false, unique = true)
    @Comment("게시글")
    private Post post;

    @Column(nullable = false, length = 2200)
    @Comment("질문 내용 (제목 + 본문 스냅샷)")
    private String questionContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Comment("처리 상태")
    private AiQueueStatus status;

    @Column(nullable = false)
    @Comment("재시도 횟수")
    private Integer retryCount = 0;

    @Column(length = 500)
    @Comment("에러 메시지")
    private String errorMessage;

    @Comment("처리 완료 시각")
    private LocalDateTime processedAt;

    @Column(nullable = false, updatable = false)
    @Comment("생성일시")
    private LocalDateTime createdAt;

    // ==================== 생성 ====================

    public static AiAnswerQueue create(Post post) {
        AiAnswerQueue queue = new AiAnswerQueue();
        queue.post = post;
        queue.questionContent = buildQuestionContent(post);
        queue.status = AiQueueStatus.PENDING;
        queue.retryCount = 0;
        queue.createdAt = LocalDateTime.now();
        return queue;
    }

    private static String buildQuestionContent(Post post) {
        return "[제목] " + post.getTitle() + "\n\n[내용]\n" + post.getContent();
    }

    // ==================== 상태 변경 ====================

    public void markProcessing() {
        this.status = AiQueueStatus.PROCESSING;
    }

    public void markCompleted() {
        this.status = AiQueueStatus.COMPLETED;
        this.processedAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.retryCount++;
        this.errorMessage = truncateMessage(errorMessage);
        this.status = canRetry() ? AiQueueStatus.PENDING : AiQueueStatus.FAILED;
    }

    public boolean canRetry() {
        return retryCount < AiConstants.MAX_RETRY_COUNT;
    }

    private String truncateMessage(String message) {
        if (message == null) return null;
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
