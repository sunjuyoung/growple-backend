package com.grow.payment.application.port.out;

public interface PaymentStatusUpdatePort {
    boolean updatePaymentStatusToExecuting(String orderId, String paymentKey);
    boolean updatePaymentStatus(PaymentStatusUpdateCommand command);
}
