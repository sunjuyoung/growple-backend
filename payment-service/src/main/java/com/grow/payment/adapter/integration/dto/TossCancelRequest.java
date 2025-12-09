package com.grow.payment.adapter.integration.dto;

/**
 * 토스 결제 취소 요청
 */
public record TossCancelRequest(
        String cancelReason
) {}
