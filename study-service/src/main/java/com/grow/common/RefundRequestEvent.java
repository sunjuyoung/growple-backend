package com.grow.common;

public record RefundRequestEvent(
        Long userId,
        Long studyId,
        String orderName,
        Integer amount,
        String paymentKey,
        String reason
) {
    public static RefundRequestEvent of(
            Long userId,
            Long studyId,
            String orderName,
            Integer amount,
            String paymentKey,
            String reason
    ) {
        return new RefundRequestEvent(
                userId,
                studyId,
                orderName,
                amount,
                paymentKey,
                reason
        );
    }
}
