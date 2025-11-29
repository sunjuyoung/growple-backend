package com.grow.payment.application.port.out;

import com.grow.payment.domain.PaymentExecutionResult;
import com.grow.payment.domain.PaymentExtraDetails;
import com.grow.payment.domain.PaymentFailure;
import com.grow.payment.domain.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusUpdateCommand {
    private String paymentKey;
    private String orderId;
    private PaymentStatus status;
    private PaymentExtraDetails extraDetails;
    private PaymentFailure failure;

    public PaymentStatusUpdateCommand(PaymentExecutionResult paymentExecutionResult) {
        this.paymentKey = paymentExecutionResult.getPaymentKey();
        this.orderId = paymentExecutionResult.getOrderId();
        this.status = paymentExecutionResult.paymentStatus();
        this.extraDetails = paymentExecutionResult.getExtraDetails();
        this.failure = paymentExecutionResult.getFailure();

        validate();
    }

    private void validate() {
        if (status != PaymentStatus.SUCCESS && status != PaymentStatus.FAILURE && status != PaymentStatus.UNKNOWN) {
            throw new IllegalArgumentException("결제 상태 (status: " + status + ") 는 올바르지 않은 결제 상태입니다.");
        }

        if (status == PaymentStatus.SUCCESS && extraDetails == null) {
            throw new IllegalArgumentException("PaymentStatus 값이 SUCCESS 라면 PaymentExtraDetails 는 null 이 되면 안됩니다.");
        }

        if (status == PaymentStatus.FAILURE && failure == null) {
            throw new IllegalArgumentException("PaymentStatus 값이 FAILURE 라면 PaymentExecutionFailure 는 null 이 되면 안됩니다.");
        }
    }
}
