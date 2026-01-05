package com.grow.payment.adapter.integration.dto;

import java.time.LocalDateTime;

/**
 * 포인트 환급 응답 DTO
 * Member Service의 Internal API 응답 매핑
 */
public record PointRefundResponse(
        Long memberId,
        Integer refundedAmount,
        Integer currentPoint,
        Long settlementItemId,
        LocalDateTime processedAt,
        boolean success,
        String message
) {}
