package com.grow.study.domain.scheduler;

import com.grow.study.domain.AbstractEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "scheduler_job",
        indexes = {
                @Index(name = "idx_job_type_status", columnList = "job_type,status,next_retry_at"),
                @Index(name = "idx_job_target", columnList = "target_type,target_id"),
                @Index(name = "idx_job_scheduled_date", columnList = "scheduled_date")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_job_target",
                        columnNames = {"job_type", "target_type", "target_id", "scheduled_date"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SchedulerJob extends AbstractEntity {

    private static final int DEFAULT_MAX_RETRIES = 3;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 30)
    private JobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status = JobStatus.PENDING;

    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "max_retries", nullable = false)
    private int maxRetries = DEFAULT_MAX_RETRIES;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Version
    private long version;

    public static SchedulerJob create(JobType jobType, String targetType, Long targetId, LocalDate scheduledDate) {
        SchedulerJob job = new SchedulerJob();
        job.jobType = jobType;
        job.targetType = targetType;
        job.targetId = targetId;
        job.scheduledDate = scheduledDate;
        job.status = JobStatus.PENDING;
        job.createdAt = LocalDateTime.now();
        return job;
    }

    /**
     * 배치 선점(Claim) - 낙관적 락으로 동시성 제어
     */
    public void claim(LocalDateTime now) {
        if (!status.isClaimable()) {
            throw new IllegalStateException("Job is not claimable: " + status);
        }
        if (nextRetryAt != null && nextRetryAt.isAfter(now)) {
            throw new IllegalStateException("Job is not due yet");
        }
        if (!isRetryable()) {
            throw new IllegalStateException("Job has exceeded max retries");
        }
        this.status = JobStatus.PROCESSING;
        this.processingStartedAt = now;
        this.lastError = null;
    }

    /**
     * 처리 완료
     */
    public void complete(LocalDateTime now) {
        this.status = JobStatus.COMPLETED;
        this.completedAt = now;
        this.processingStartedAt = null;
        this.nextRetryAt = null;
        this.lastError = null;
    }

    /**
     * 처리 실패 - 재시도 스케줄링
     */
    public void fail(String error, LocalDateTime nextRetryAt) {
        this.status = JobStatus.FAILED;
        this.retryCount++;
        this.nextRetryAt = nextRetryAt;
        this.lastError = truncate(error);
        this.processingStartedAt = null;
    }

    /**
     * 재시도 가능 여부
     */
    public boolean isRetryable() {
        return retryCount < maxRetries;
    }

    /**
     * 재시도 횟수 초과 여부
     */
    public boolean isExhausted() {
        return status == JobStatus.FAILED && retryCount >= maxRetries;
    }

    private String truncate(String s) {
        if (s == null) return null;
        return s.length() > 1000 ? s.substring(0, 1000) : s;
    }
}
