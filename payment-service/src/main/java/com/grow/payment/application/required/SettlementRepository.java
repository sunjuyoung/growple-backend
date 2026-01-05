package com.grow.payment.application.required;

import com.grow.payment.domain.settlement.Settlement;
import com.grow.payment.domain.settlement.SettlementStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 정산 리포지토리 포트 (Output Port)
 */
public interface SettlementRepository {

    Settlement save(Settlement settlement);

    Optional<Settlement> findById(Long id);

    Optional<Settlement> findByStudyId(Long studyId);

    boolean existsByStudyId(Long studyId);

    /**
     * 처리 대상 정산 조회
     * PENDING 또는 FAILED 상태이면서 재시도 시간이 지난 건
     */
    List<Settlement> findSettlementsToProcess(LocalDateTime now);

    List<Settlement> findByStatus(SettlementStatus status);

    /**
     * 이미 정산이 생성된 스터디 ID 목록 조회
     */
    List<Long> findStudyIdsWithSettlement(List<Long> studyIds);
}
