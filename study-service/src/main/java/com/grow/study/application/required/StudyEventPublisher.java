package com.grow.study.application.required;

import com.grow.common.PaymentEnrollmentEvent;
import com.grow.common.StudyCreateEvent;

public interface StudyEventPublisher {

     void publishStudy(StudyCreateEvent message);

     void publishStudyEnrolledFailedEvent(PaymentEnrollmentEvent event, String message);
}
