package com.grow.common;

public record PaymentEnrollmentEvent(
        Long userId,
        Long studyId,
        String orderName,
        Integer amount,
        String paymentKey
) {
    public static PaymentEnrollmentEvent of(Long userId, Long studyId, String orderName, Integer amount, String paymentKey) {
        return new PaymentEnrollmentEvent(userId, studyId, orderName, amount, paymentKey);
    }
}
