package com.grow.payment.domain.settlement;


import com.grow.payment.domain.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "settlement",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_settlement_study", columnNames = {"study_id"})
        },
        indexes = {
                @Index(name = "idx_settlement_status_retry", columnList = "status,next_retry_at"),
                @Index(name = "idx_settlement_processing_started", columnList = "processing_started_at")
        }
)
@Getter
public class Settlement extends AbstractEntity {

    /** 다른 애그리게이트와 결합을 피하기 위해 ID만 보관 */
    @Column(name = "study_id", nullable = false)
    private Long studyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementStatus status = SettlementStatus.PENDING;

    /** lease */
    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    /** retry */
    @Column(nullable = false)
    private int retryCount = 0;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    /** 동시성(낙관적 락): 포트폴리오에서도 설명 포인트가 됨 */
    @Version
    private long version;

    protected Settlement() {}

    public static Settlement create(Long studyId) {
        Settlement s = new Settlement();
        s.studyId = studyId;
        s.status = SettlementStatus.PENDING;
        return s;
    }

    /** 배치 선점(Claim) 의미의 상태 전이 */
    public void claim(LocalDateTime now) {
        if (!(status == SettlementStatus.PENDING || status == SettlementStatus.FAILED)) {
            throw new IllegalStateException("settlement is not claimable: " + status);
        }
        if (nextRetryAt != null && nextRetryAt.isAfter(now)) {
            throw new IllegalStateException("not due yet");
        }
        this.status = SettlementStatus.PROCESSING;
        this.processingStartedAt = now;
        this.lastError = null;
    }

    /** 아이템 일부 실패가 남으면 헤더도 FAILED로 두고 재시도 스케줄링 */
    public void markFailed(LocalDateTime now, int nextRetryCount, LocalDateTime nextRetryAt, String error) {
        this.status = SettlementStatus.FAILED;
        this.retryCount = nextRetryCount;
        this.nextRetryAt = nextRetryAt;
        this.lastError = truncate(error);
        this.processingStartedAt = null;
    }

    public void complete(LocalDateTime now) {
        this.status = SettlementStatus.COMPLETED;
        this.settledAt = now;
        this.processingStartedAt = null;
        this.nextRetryAt = null;
        this.lastError = null;
    }



    private String truncate(String s) {
        if (s == null) return null;
        return s.length() > 1000 ? s.substring(0, 1000) : s;
    }
}
