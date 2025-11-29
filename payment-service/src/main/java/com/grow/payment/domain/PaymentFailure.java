package com.grow.payment.domain;

public class PaymentFailure {
    private  String errorCode;
    private  String message;

    public PaymentFailure(String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }
}
