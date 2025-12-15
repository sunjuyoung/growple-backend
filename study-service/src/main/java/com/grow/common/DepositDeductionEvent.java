package com.grow.common;

/**
 * 보증금 차감 이벤트
 * 결석 시 Payment 서비스로 전달
 */
public record DepositDeductionEvent(
        Long studyId,
        Long memberId,
        Long sessionId,
        Integer sessionNumber,
        Integer deductionAmount,
        String reason
) {
    public static DepositDeductionEvent ofAbsence(
            Long studyId,
            Long memberId,
            Long sessionId,
            Integer sessionNumber
    ) {
        return new DepositDeductionEvent(
                studyId,
                memberId,
                sessionId,
                sessionNumber,
                1000,  // 결석 시 1,000원 차감
                "결석"
        );
    }
}
