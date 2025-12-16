package com.grow.study.application.required;

import com.grow.common.DepositDeductionEvent;
import com.grow.common.PaymentEnrollmentEvent;
import com.grow.common.StudyCreateEvent;

public interface StudyEventPublisher {

     void publishStudyEnrolledFailedEvent(PaymentEnrollmentEvent event, String message);

     void publishStudyMember(StudyCreateEvent message);
}
