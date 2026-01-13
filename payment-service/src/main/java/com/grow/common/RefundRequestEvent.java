package com.grow.common;

import java.util.UUID;

public record RefundRequestEvent(
        String eventId,
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
                UUID.randomUUID().toString(),
                userId,
                studyId,
                orderName,
                amount,
                paymentKey,
                reason
        );
    }
}
