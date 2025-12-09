package com.grow.payment.adapter.integration;

/**
 * 토스 결제 API 호출 실패 시 예외
 */
public class TossPaymentException extends RuntimeException {
    
    public TossPaymentException(String message) {
        super(message);
    }
    
    public TossPaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
