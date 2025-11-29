package com.grow.payment.domain.enums;

import java.util.Arrays;

public enum PaymentType {
    NORMAL("일반 결제");

    private final String description;

    PaymentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static PaymentType get(String type) {
        return Arrays.stream(values())
                .filter(paymentType -> paymentType.name().equals(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("PaymentType (type: " + type + ") 은 올바르지 않은 결제 타입입니다."));
    }
}
