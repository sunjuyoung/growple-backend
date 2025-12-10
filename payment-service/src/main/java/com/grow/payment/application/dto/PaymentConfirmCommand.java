package com.grow.payment.application.dto;

/**
 * 결제 승인 커맨드
 * - 토스 결제창에서 redirect 후 전달받는 값
 */
public record PaymentConfirmCommand(
        String paymentKey,  // 토스가 발급한 결제 키
        String orderId,     // 우리가 생성한 주문 ID
        Integer amount,      // 결제 금액 (검증용)
        Integer studyId
) {
    public static PaymentConfirmCommand of(String paymentKey, String orderId, Integer amount, Integer studyId) {
        return new PaymentConfirmCommand(paymentKey, orderId, amount, studyId);
    }
}
