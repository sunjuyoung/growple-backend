package com.grow.payment.adapter.config;

import com.grow.payment.application.dto.CompletedStudyForSettlementResponse;

import java.util.List;

/**
 * 배치 처리용 스터디 DTO
 * Study Service API 응답을 배치에서 사용할 수 있는 형태로 래핑
 */
public record ExpiredStudyDto(
        Long studyId,
        String title,
        Integer depositAmount,
        Integer penaltyPerAbsence,
        List<ParticipantDto> participants
) {
    public record ParticipantDto(
            Long participantId,
            Long memberId,
            Integer depositPaid,
            Integer absenceCount,
            Integer attendanceCount
    ) {
        public static ParticipantDto from(CompletedStudyForSettlementResponse.ParticipantForSettlement p) {
            return new ParticipantDto(
                    p.participantId(),
                    p.memberId(),
                    p.depositPaid(),
                    p.absenceCount(),
                    p.attendanceCount()
            );
        }
    }

    public static ExpiredStudyDto from(CompletedStudyForSettlementResponse response) {
        return new ExpiredStudyDto(
                response.studyId(),
                response.title(),
                response.depositAmount(),
                response.penaltyPerAbsence(),
                response.participants().stream()
                        .map(ParticipantDto::from)
                        .toList()
        );
    }
}
