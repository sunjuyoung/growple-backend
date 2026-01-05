package com.grow.study.adapter.webapi.dto.settlement;

import java.util.List;

/**
 * 정산 대상 스터디 응답 DTO
 * Payment Service의 배치에서 사용
 */
public record CompletedStudyForSettlementResponse(
        Long studyId,
        String title,
        Integer depositAmount,
        Integer penaltyPerAbsence,  // 결석당 차감 금액
        List<ParticipantForSettlement> participants
) {
    
    public record ParticipantForSettlement(
            Long participantId,     // StudyMember의 ID
            Long memberId,          // 회원 ID (Member Service의 ID)
            Integer depositPaid,    // 납부한 보증금
            Integer absenceCount,   // 결석 횟수
            Integer attendanceCount // 출석 횟수
    ) {}
}
