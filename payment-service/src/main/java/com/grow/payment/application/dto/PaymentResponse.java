package com.grow.payment.application.dto;

import com.grow.payment.domain.Payment;
import com.grow.payment.domain.enums.PaymentStatus;

import java.time.LocalDateTime;

/**
 * 결제 응답 DTO
 */
public record PaymentResponse(
        Long id,
        Long memberId,
        Long studyId,
        String orderId,
        String orderName,
        Integer amount,
        String paymentKey,
        String method,
        PaymentStatus status,
        LocalDateTime requestedAt,
        LocalDateTime approvedAt,
        LocalDateTime cancelledAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getMemberId(),
                payment.getStudyId(),
                payment.getOrderId(),
                payment.getOrderName(),
                payment.getAmount(),
                payment.getPaymentKey(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getRequestedAt(),
                payment.getApprovedAt(),
                payment.getCancelledAt()
        );
    }
}
