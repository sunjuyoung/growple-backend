package com.grow.payment.domain;

import com.grow.payment.domain.enums.PSPConfirmationStatus;
import com.grow.payment.domain.enums.PaymentMethod;
import com.grow.payment.domain.enums.PaymentType;

import java.time.LocalDateTime;

public class PaymentExtraDetails {
    private PaymentType type;
    private PaymentMethod method;
    private LocalDateTime approvedAt;
    private String orderName;
    private PSPConfirmationStatus pspConfirmationStatus;
    private Long totalAmount;
    private String pspRawData;

    public PaymentExtraDetails(PaymentType type, PaymentMethod method, LocalDateTime approvedAt,
                              String orderName, PSPConfirmationStatus pspConfirmationStatus,
                              Long totalAmount, String pspRawData) {
        this.type = type;
        this.method = method;
        this.approvedAt = approvedAt;
        this.orderName = orderName;
        this.pspConfirmationStatus = pspConfirmationStatus;
        this.totalAmount = totalAmount;
        this.pspRawData = pspRawData;
    }

    public PaymentType getType() {
        return type;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public String getOrderName() {
        return orderName;
    }

    public PSPConfirmationStatus getPspConfirmationStatus() {
        return pspConfirmationStatus;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public String getPspRawData() {
        return pspRawData;
    }
}
