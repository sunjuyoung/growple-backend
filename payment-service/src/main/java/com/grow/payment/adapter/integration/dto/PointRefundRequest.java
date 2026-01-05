package com.grow.payment.adapter.integration.dto;

/**
 * 포인트 환급 요청 DTO
 * Member Service의 Internal API에 전송
 */
public record PointRefundRequest(
        Integer amount,
        Long settlementItemId,
        String reason
) {}
