package com.grow.member.adapter.webapi.dto.internal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 포인트 환급 요청 DTO
 * Payment Service의 정산 배치에서 사용
 */
public record PointRefundRequest(
        @NotNull(message = "환급 금액은 필수입니다")
        @Positive(message = "환급 금액은 0보다 커야 합니다")
        Integer amount,

        @NotNull(message = "정산 아이템 ID는 필수입니다")
        Long settlementItemId,

        String reason  // 환급 사유 (선택)
) {
    public static PointRefundRequest of(Integer amount, Long settlementItemId, String reason) {
        return new PointRefundRequest(amount, settlementItemId, reason);
    }
}
