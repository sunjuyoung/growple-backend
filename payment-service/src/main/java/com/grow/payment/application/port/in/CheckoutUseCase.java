package com.grow.payment.application.port.in;

import com.grow.payment.domain.CheckoutResult;

public interface CheckoutUseCase {
    CheckoutResult checkout(CheckoutCommand command);
}
