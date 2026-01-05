package com.grow.payment.adapter.persistence;

import com.grow.payment.domain.settlement.Settlement;
import com.grow.payment.domain.settlement.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SettlementJpaRepository extends JpaRepository<Settlement, Long> {

    /**
     * 특정 스터디의 정산 존재 여부 확인
     */
    boolean existsByStudyId(Long studyId);

    /**
     * 스터디 ID로 정산 조회
     */
    Optional<Settlement> findByStudyId(Long studyId);

    /**
     * 정산 대상 조회 (PENDING 또는 FAILED 상태이면서 재시도 시간이 지난 건)
     */
    @Query("""
        SELECT s FROM Settlement s
        WHERE (s.status = :pendingStatus OR s.status = :failedStatus)
        AND (s.nextRetryAt IS NULL OR s.nextRetryAt <= :now)
        ORDER BY s.createdAt ASC
    """)
    List<Settlement> findSettlementsToProcess(
            @Param("pendingStatus") SettlementStatus pendingStatus,
            @Param("failedStatus") SettlementStatus failedStatus,
            @Param("now") LocalDateTime now
    );

    /**
     * 특정 상태의 정산 목록 조회
     */
    List<Settlement> findByStatus(SettlementStatus status);

    /**
     * 스터디 ID 목록으로 이미 정산이 생성된 스터디 조회
     */
    @Query("SELECT s.studyId FROM Settlement s WHERE s.studyId IN :studyIds")
    List<Long> findStudyIdsWithSettlement(@Param("studyIds") List<Long> studyIds);
}
