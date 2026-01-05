package com.grow.payment.application.dto;

import java.util.List;

/**
 * 정산 대상 스터디 응답 DTO
 * Study Service의 Internal API 응답을 매핑
 */
public record CompletedStudyForSettlementResponse(
        Long studyId,
        String title,
        Integer depositAmount,
        Integer penaltyPerAbsence,
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
