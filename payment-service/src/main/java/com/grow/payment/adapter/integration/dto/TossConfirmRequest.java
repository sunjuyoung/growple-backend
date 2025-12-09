package com.grow.payment.adapter.integration.dto;

/**
 * 토스 결제 승인 요청
 */
public record TossConfirmRequest(
        String paymentKey,
        String orderId,
        Integer amount
) {}
