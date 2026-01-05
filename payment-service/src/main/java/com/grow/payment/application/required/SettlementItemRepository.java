package com.grow.payment.application.required;

import com.grow.payment.domain.settlement.SettlementItem;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 정산 아이템 리포지토리 포트 (Output Port)
 */
public interface SettlementItemRepository {

    SettlementItem save(SettlementItem item);

    List<SettlementItem> saveAll(List<SettlementItem> items);

    Optional<SettlementItem> findById(Long id);

    List<SettlementItem> findBySettlementId(Long settlementId);

    /**
     * 처리 대상 아이템 조회
     */
    List<SettlementItem> findItemsToProcess(Long settlementId, LocalDateTime now);

    /**
     * 모든 아이템이 완료되었는지 확인
     */
    boolean areAllItemsCompleted(Long settlementId);

    /**
     * 멤버의 정산 이력 조회
     */
    List<SettlementItem> findByMemberId(Long memberId);
}
