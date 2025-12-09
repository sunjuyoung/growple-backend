package com.grow.payment.application.dto;

/**
 * 결제 요청 커맨드
 */
public record PaymentRequestCommand(
        Long memberId,
        Long studyId,
        String orderName,
        Integer amount
) {
    public static PaymentRequestCommand of(Long memberId, Long studyId, String orderName, Integer amount) {
        return new PaymentRequestCommand(memberId, studyId, orderName, amount);
    }
}
