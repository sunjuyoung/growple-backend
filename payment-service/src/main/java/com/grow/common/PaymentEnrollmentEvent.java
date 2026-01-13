package com.grow.common;

import java.util.UUID;

public record PaymentEnrollmentEvent(
        String eventId,
        Long userId,
        Long studyId,
        String orderName,
        Integer amount,
        String paymentKey
) {
    public static PaymentEnrollmentEvent of(Long userId, Long studyId, String orderName, Integer amount, String paymentKey) {
        return new PaymentEnrollmentEvent(
                UUID.randomUUID().toString(),
                userId,
                studyId,
                orderName,
                amount,
                paymentKey
        );
    }
}
