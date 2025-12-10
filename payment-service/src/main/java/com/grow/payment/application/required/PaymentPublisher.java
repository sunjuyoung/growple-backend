package com.grow.payment.application.required;

import com.grow.common.PaymentEnrollmentEvent;

public interface PaymentPublisher {

     void publishPaymentEnrolledEvent(PaymentEnrollmentEvent message);
}
