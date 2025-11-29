package com.grow.payment.domain;

import com.grow.payment.domain.enums.PaymentStatus;

public class PendingPaymentOrder {
    private Long paymentOrderId;
    private PaymentStatus status;
    private Long amount;
    private Byte failedCount;
    private Byte threshold;

    public PendingPaymentOrder(Long paymentOrderId, PaymentStatus status, Long amount,
                               Byte failedCount, Byte threshold) {
        this.paymentOrderId = paymentOrderId;
        this.status = status;
        this.amount = amount;
        this.failedCount = failedCount;
        this.threshold = threshold;
    }

    public Long getPaymentOrderId() {
        return paymentOrderId;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Long getAmount() {
        return amount;
    }

    public Byte getFailedCount() {
        return failedCount;
    }

    public Byte getThreshold() {
        return threshold;
    }
}
