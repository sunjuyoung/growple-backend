package com.grow.payment.application.port.in;

import com.grow.payment.domain.PaymentConfirmationResult;

public interface PaymentConfirmUseCase {
    PaymentConfirmationResult confirm(PaymentConfirmCommand command);
}
