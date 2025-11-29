package com.grow.payment.application.port.out;

import com.grow.payment.application.port.in.PaymentConfirmCommand;
import com.grow.payment.domain.PaymentExecutionResult;

public interface PaymentExecutorPort {
    PaymentExecutionResult execute(PaymentConfirmCommand command);
}
