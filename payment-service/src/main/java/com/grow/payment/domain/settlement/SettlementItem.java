package com.grow.payment.domain.settlement;


import com.grow.payment.domain.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "settlement_item",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_item_settlement_participant", columnNames = {"settlement_id","participant_id"})
        },
        indexes = {
                @Index(name = "idx_item_settlement_status_retry", columnList = "settlement_id,status,next_retry_at"),
                @Index(name = "idx_item_member", columnList = "member_id")
        }
)
@Getter
public class SettlementItem extends AbstractEntity {

    @Column(name = "settlement_id", nullable = false)
    private Long settlementId;

    /** 정산 컨텍스트는 외부 엔티티를 직접 참조하지 않고 ID로만 들고감 */
    @Column(name = "participant_id", nullable = false)
    private Long participantId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** 정산 시점에 “원금(보증금)”을 스냅샷으로 고정 */
    @Column(name = "original_amount", nullable = false)
    private long originalAmount;

    @Column(name = "absence_count", nullable = false)
    private int absenceCount;

    @Column(name = "penalty_amount", nullable = false)
    private long penaltyAmount;

    @Column(name = "refund_amount", nullable = false)
    private long refundAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payout_method", nullable = false, length = 20)
    private PayoutMethod payoutMethod = PayoutMethod.POINT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementItemStatus status = SettlementItemStatus.PENDING;

    /** retry */
    @Column(nullable = false)
    private int retryCount = 0;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    /** 지급 추적(선택): point_transaction의 id를 저장 */
    @Column(name = "processed_point_tx_id")
    private Long processedPointTxId;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Version
    private long version;

    protected SettlementItem() {}

    public static SettlementItem create(Long settlementId, Long participantId, Long memberId, long originalAmount) {
        SettlementItem i = new SettlementItem();
        i.settlementId = settlementId;
        i.participantId = participantId;
        i.memberId = memberId;
        i.originalAmount = originalAmount;
        i.status = SettlementItemStatus.PENDING;
        return i;
    }

    /** 출석 집계 결과를 반영해 정산 금액 확정(멱등: 같은 입력이면 같은 결과) */
    public void applyAttendanceResult(int absenceCount, long penaltyPerAbsence) {
        if (status == SettlementItemStatus.PAYOUT_DONE) return; // 이미 지급 완료면 변경 금지(안전)

        if (absenceCount < 0) throw new IllegalArgumentException("absenceCount");
        this.absenceCount = absenceCount;

        long penalty = absenceCount * penaltyPerAbsence;
        if (penalty < 0) penalty = 0;

        this.penaltyAmount = Math.min(penalty, originalAmount);
        this.refundAmount = originalAmount - this.penaltyAmount;
    }

    public void markPaid(LocalDateTime now, Long pointTxIdNullable) {
        this.status = SettlementItemStatus.PAYOUT_DONE;
        this.processedAt = now;
        this.processedPointTxId = pointTxIdNullable;
        this.nextRetryAt = null;
        this.lastError = null;
    }

    public void markFailed(LocalDateTime now, int nextRetryCount, LocalDateTime nextRetryAt, String error) {
        this.status = SettlementItemStatus.FAILED;
        this.retryCount = nextRetryCount;
        this.nextRetryAt = nextRetryAt;
        this.lastError = truncate(error);
        this.processedAt = null;
    }

    public boolean isDue(LocalDateTime now) {
        return (status == SettlementItemStatus.PENDING || status == SettlementItemStatus.FAILED)
                && (nextRetryAt == null || !nextRetryAt.isAfter(now));
    }



    private String truncate(String s) {
        if (s == null) return null;
        return s.length() > 1000 ? s.substring(0, 1000) : s;
    }
}
