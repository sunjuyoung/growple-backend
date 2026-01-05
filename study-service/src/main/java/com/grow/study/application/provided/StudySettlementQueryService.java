package com.grow.study.application.provided;

import com.grow.study.adapter.webapi.dto.settlement.CompletedStudyForSettlementResponse;
import com.grow.study.adapter.webapi.dto.settlement.CompletedStudyForSettlementResponse.ParticipantForSettlement;
import com.grow.study.application.required.StudyRepository;
import com.grow.study.domain.study.Study;
import com.grow.study.domain.study.StudyMember;
import com.grow.study.domain.study.StudyMemberStatus;
import com.grow.study.domain.study.StudyStatus;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 정산용 스터디 조회 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudySettlementQueryService {

    private final StudyRepository studyRepository;

    // 결석당 차감 금액 (추후 스터디별 설정으로 변경 가능)
    private static final int DEFAULT_PENALTY_PER_ABSENCE = 1000;

    /**
     * 정산 대상 스터디 조회
     * COMPLETED 상태인 스터디만 조회 (SETTLED 제외)
     */
    public List<CompletedStudyForSettlementResponse> findCompletedStudiesForSettlement(int limit) {
        List<Study> completedStudies = studyRepository.findByStatus(StudyStatus.COMPLETED);
        
        return completedStudies.stream()
                .limit(limit)
                .map(this::toSettlementResponse)
                .collect(Collectors.toList());
    }

    private CompletedStudyForSettlementResponse toSettlementResponse(Study study) {
        List<ParticipantForSettlement> participants = study.getMembers().stream()
                .filter(member -> member.getStatus() == StudyMemberStatus.ACTIVE)
                .map(this::toParticipantResponse)
                .collect(Collectors.toList());

        return new CompletedStudyForSettlementResponse(
                study.getId(),
                study.getTitle(),
                study.getDepositAmount(),
                DEFAULT_PENALTY_PER_ABSENCE,
                participants
        );
    }

    private ParticipantForSettlement toParticipantResponse(StudyMember member) {
        return new ParticipantForSettlement(
                member.getId(),
                member.getMemberId(),
                member.getDepositPaid(),
                member.getAbsenceCount(),
                member.getAttendanceCount()
        );
    }

    /**
     * 스터디 정산 완료 처리
     */
    @Transactional
    public void markAsSettled(Long studyId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new EntityNotFoundException("스터디를 찾을 수 없습니다: " + studyId));

        if (study.getStatus() != StudyStatus.COMPLETED) {
            throw new IllegalStateException(
                    "COMPLETED 상태의 스터디만 정산 완료 처리할 수 있습니다. 현재 상태: " + study.getStatus()
            );
        }

        // Study 엔티티에 markAsSettled 메서드가 필요합니다
        study.markAsSettled();
        
        log.info("스터디 {} 정산 완료 처리됨", studyId);
    }
}
