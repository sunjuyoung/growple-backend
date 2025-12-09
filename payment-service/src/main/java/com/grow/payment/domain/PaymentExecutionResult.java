package com.grow.payment.domain;

import com.grow.payment.domain.enums.PaymentStatus;

public class PaymentExecutionResult {
    private String paymentKey;
    private String orderId;
    private PaymentExtraDetails extraDetails;
    private PaymentFailure failure;
    private boolean isSuccess;
    private boolean isFailure;
    private boolean isUnknown;
    private boolean isRetryable;

    public PaymentExecutionResult(String paymentKey, String orderId, PaymentExtraDetails extraDetails,
                                 PaymentFailure failure, boolean isSuccess, boolean isFailure,
                                 boolean isUnknown, boolean isRetryable) {
        if (!isSuccess && !isFailure && !isUnknown) {
            throw new IllegalArgumentException("결제 (orderId: " + orderId + ") 는 올바르지 않은 결제 상태입니다.");
        }
        this.paymentKey = paymentKey;
        this.orderId = orderId;
        this.extraDetails = extraDetails;
        this.failure = failure;
        this.isSuccess = isSuccess;
        this.isFailure = isFailure;
        this.isUnknown = isUnknown;
        this.isRetryable = isRetryable;
    }

    public String getPaymentKey() {
        return paymentKey;
    }

    public String getOrderId() {
        return orderId;
    }

    public PaymentExtraDetails getExtraDetails() {
        return extraDetails;
    }

    public PaymentFailure getFailure() {
        return failure;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public boolean isFailure() {
        return isFailure;
    }

    public boolean isUnknown() {
        return isUnknown;
    }

    public boolean isRetryable() {
        return isRetryable;
    }

    public PaymentStatus paymentStatus() {
        if (isSuccess) {
            return PaymentStatus.DONE;
        } else if (isFailure) {
            return PaymentStatus.FAILED;
        } else if (isUnknown) {
            return PaymentStatus.FAILED;
        } else {
            throw new IllegalArgumentException("결제 (orderId: " + orderId + ") 는 올바르지 않은 결제 상태입니다.");
        }
    }
}
