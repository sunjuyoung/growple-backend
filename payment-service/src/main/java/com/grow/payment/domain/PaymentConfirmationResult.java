package com.grow.payment.domain;

import com.grow.payment.domain.enums.PaymentStatus;

public class PaymentConfirmationResult {
    private  PaymentStatus status;
    private  PaymentFailure failure;
    private  String message;

    public PaymentConfirmationResult(PaymentStatus status, PaymentFailure failure) {
        if (status == PaymentStatus.FAILURE && failure == null) {
            throw new IllegalArgumentException("결제 상태 FAILURE 일 때 PaymentExecutionFailure 는 null 값이 될 수 없습니다.");
        }
        this.status = status;
        this.failure = failure;
        this.message = generateMessage(status);
    }

    public PaymentConfirmationResult(PaymentStatus status) {
        this(status, null);
    }

    private String generateMessage(PaymentStatus status) {
        switch (status) {
            case SUCCESS:
                return "결제 처리에 성공하였습니다.";
            case FAILURE:
                return "결제 처리에 실패하였습니다.";
            case UNKNOWN:
                return "결제 처리 중에 알 수 없는 에러가 발생하였습니다.";
            default:
                throw new IllegalArgumentException("현재 결제 상태 (status: " + status + ") 는 올바르지 않은 상태입니다.");
        }
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public PaymentFailure getFailure() {
        return failure;
    }

    public String getMessage() {
        return message;
    }
}
