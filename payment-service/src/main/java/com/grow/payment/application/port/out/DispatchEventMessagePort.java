package com.grow.payment.application.port.out;

import com.grow.payment.domain.PaymentEventMessage;

public interface DispatchEventMessagePort {
    void dispatch(PaymentEventMessage paymentEventMessage);
}
