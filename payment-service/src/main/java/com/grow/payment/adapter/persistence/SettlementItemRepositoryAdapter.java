package com.grow.payment.adapter.persistence;

import com.grow.payment.application.required.SettlementItemRepository;
import com.grow.payment.domain.settlement.SettlementItem;
import com.grow.payment.domain.settlement.SettlementItemStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SettlementItemRepositoryAdapter implements SettlementItemRepository {

    private final SettlementItemJpaRepository jpaRepository;

    @Override
    public SettlementItem save(SettlementItem item) {
        return jpaRepository.save(item);
    }

    @Override
    public List<SettlementItem> saveAll(List<SettlementItem> items) {
        return jpaRepository.saveAll(items);
    }

    @Override
    public Optional<SettlementItem> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<SettlementItem> findBySettlementId(Long settlementId) {
        return jpaRepository.findBySettlementId(settlementId);
    }

    @Override
    public List<SettlementItem> findItemsToProcess(Long settlementId, LocalDateTime now) {
        return jpaRepository.findItemsToProcess(
                settlementId,
                SettlementItemStatus.PENDING,
                SettlementItemStatus.FAILED,
                now
        );
    }

    @Override
    public boolean areAllItemsCompleted(Long settlementId) {
        return jpaRepository.areAllItemsCompleted(settlementId);
    }

    @Override
    public List<SettlementItem> findByMemberId(Long memberId) {
        return jpaRepository.findByMemberId(memberId);
    }
}
