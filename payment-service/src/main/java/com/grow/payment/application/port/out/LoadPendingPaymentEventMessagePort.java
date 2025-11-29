package com.grow.payment.application.port.out;

import com.grow.payment.domain.PaymentEventMessage;

import java.util.List;

public interface LoadPendingPaymentEventMessagePort {
    List<PaymentEventMessage> getPendingPaymentEventMessage();
}
