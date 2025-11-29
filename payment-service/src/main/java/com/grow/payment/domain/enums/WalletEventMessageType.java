package com.grow.payment.domain.enums;

public enum WalletEventMessageType {
    SUCCESS("정산 처리 성공");

    private final String description;

    WalletEventMessageType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
