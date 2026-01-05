package com.grow.payment.adapter.persistence;

import com.grow.payment.application.required.SettlementRepository;
import com.grow.payment.domain.settlement.Settlement;
import com.grow.payment.domain.settlement.SettlementStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SettlementRepositoryAdapter implements SettlementRepository {

    private final SettlementJpaRepository jpaRepository;

    @Override
    public Settlement save(Settlement settlement) {
        return jpaRepository.save(settlement);
    }

    @Override
    public Optional<Settlement> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Settlement> findByStudyId(Long studyId) {
        return jpaRepository.findByStudyId(studyId);
    }

    @Override
    public boolean existsByStudyId(Long studyId) {
        return jpaRepository.existsByStudyId(studyId);
    }

    @Override
    public List<Settlement> findSettlementsToProcess(LocalDateTime now) {
        return jpaRepository.findSettlementsToProcess(
                SettlementStatus.PENDING,
                SettlementStatus.FAILED,
                now
        );
    }

    @Override
    public List<Settlement> findByStatus(SettlementStatus status) {
        return jpaRepository.findByStatus(status);
    }

    @Override
    public List<Long> findStudyIdsWithSettlement(List<Long> studyIds) {
        if (studyIds == null || studyIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findStudyIdsWithSettlement(studyIds);
    }
}
