package com.grow.payment.adapter.persistence;

import com.grow.payment.domain.settlement.SettlementItem;
import com.grow.payment.domain.settlement.SettlementItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SettlementItemJpaRepository extends JpaRepository<SettlementItem, Long> {

    /**
     * 특정 정산의 모든 아이템 조회
     */
    List<SettlementItem> findBySettlementId(Long settlementId);

    /**
     * 특정 정산의 처리 대상 아이템 조회 (PENDING 또는 FAILED 상태)
     */
    @Query("""
        SELECT si FROM SettlementItem si
        WHERE si.settlementId = :settlementId
        AND (si.status = :pendingStatus OR si.status = :failedStatus)
        AND (si.nextRetryAt IS NULL OR si.nextRetryAt <= :now)
    """)
    List<SettlementItem> findItemsToProcess(
            @Param("settlementId") Long settlementId,
            @Param("pendingStatus") SettlementItemStatus pendingStatus,
            @Param("failedStatus") SettlementItemStatus failedStatus,
            @Param("now") LocalDateTime now
    );

    /**
     * 특정 정산의 완료되지 않은 아이템 수 조회
     */
    @Query("""
        SELECT COUNT(si) FROM SettlementItem si
        WHERE si.settlementId = :settlementId
        AND si.status != :doneStatus
    """)
    long countUnfinishedItems(
            @Param("settlementId") Long settlementId,
            @Param("doneStatus") SettlementItemStatus doneStatus
    );

    /**
     * 특정 정산의 모든 아이템이 완료되었는지 확인
     */
    default boolean areAllItemsCompleted(Long settlementId) {
        return countUnfinishedItems(settlementId, SettlementItemStatus.PAYOUT_DONE) == 0;
    }

    /**
     * 멤버 ID로 정산 아이템 조회 (정산 이력 확인용)
     */
    List<SettlementItem> findByMemberId(Long memberId);
}
