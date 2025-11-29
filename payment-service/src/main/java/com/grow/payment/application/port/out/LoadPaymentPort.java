package com.grow.payment.application.port.out;

import com.grow.payment.domain.PaymentEvent;

public interface LoadPaymentPort {
    PaymentEvent getPayment(String orderId);
}
