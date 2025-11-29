package com.grow.payment.application.port.out;

public interface PaymentValidationPort {
    boolean isValid(String orderId, Long amount);
}
