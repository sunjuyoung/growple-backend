package com.grow.study.adapter.intergration;

import com.grow.common.DepositDeductionEvent;
import com.grow.common.PaymentEnrollmentEvent;
import com.grow.common.RefundRequestEvent;
import com.grow.study.application.required.StudyEventPublisher;
import com.grow.common.StudyCreateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudyProducer implements StudyEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishStudy(StudyCreateEvent message){
        kafkaTemplate.send(Topcis.STUDY_CREATED, message);
    }

    @Override
    public void publishStudyEnrolledFailedEvent(PaymentEnrollmentEvent event, String reason) {
        RefundRequestEvent refundRequestEvent = RefundRequestEvent.of(
                event.userId(),
                event.studyId(),
                event.orderName(),
                event.amount(),
                event.paymentKey(),
                reason
        );
        kafkaTemplate.send(Topcis.STUDY_ENROLLMENT_FAILED, refundRequestEvent);
    }

}
