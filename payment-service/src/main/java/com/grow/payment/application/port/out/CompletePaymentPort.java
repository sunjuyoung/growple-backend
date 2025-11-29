package com.grow.payment.application.port.out;

import com.grow.payment.domain.PaymentEvent;

public interface CompletePaymentPort {
    void complete(PaymentEvent paymentEvent);
}
