package com.grow.payment.adapter.webapi.dto;

/**
 * 결제 승인 요청 (프론트 → 백엔드)
 * 토스 결제창 완료 후 redirect 시 전달받는 값
 */
public record PaymentConfirmRequest(
        String paymentKey,  // 토스가 발급한 결제 키
        String orderId,     // 우리가 생성한 주문 ID
        Integer amount ,     // 결제 금액 (검증용)
        Integer studyId
) {}
