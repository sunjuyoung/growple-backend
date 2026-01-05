package com.grow.member.adapter.webapi.dto.internal;

import java.time.LocalDateTime;

/**
 * 포인트 환급 응답 DTO
 */
public record PointRefundResponse(
        Long memberId,
        Integer refundedAmount,
        Integer currentPoint,
        Long settlementItemId,
        LocalDateTime processedAt,
        boolean success,
        String message
) {
    public static PointRefundResponse success(Long memberId, Integer refundedAmount, 
                                               Integer currentPoint, Long settlementItemId) {
        return new PointRefundResponse(
                memberId,
                refundedAmount,
                currentPoint,
                settlementItemId,
                LocalDateTime.now(),
                true,
                "포인트 환급 완료"
        );
    }

    public static PointRefundResponse failure(Long memberId, Long settlementItemId, String message) {
        return new PointRefundResponse(
                memberId,
                0,
                null,
                settlementItemId,
                LocalDateTime.now(),
                false,
                message
        );
    }
}
