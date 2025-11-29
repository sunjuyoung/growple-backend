package com.grow.payment.application.port.out;

import com.grow.payment.domain.PendingPaymentEvent;

import java.util.List;

public interface LoadPendingPaymentPort {
    List<PendingPaymentEvent> getPendingPayments();
}
