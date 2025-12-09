package com.grow.payment.adapter.webapi.dto;

/**
 * 결제 취소 요청 (필요시 사용)
 */
public record PaymentCancelRequest(
        String cancelReason  // 취소 사유 (선택)
) {}
