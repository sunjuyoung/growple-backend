package com.grow.payment.application.required;

import com.grow.common.PaymentEnrollmentEvent;
import com.grow.common.StudyCreateEvent;

public interface PaymentPublisher {

     void publishPaymentEnrolledEvent(PaymentEnrollmentEvent message);

     void publishStudyCreatedEvent(StudyCreateEvent message);
}
