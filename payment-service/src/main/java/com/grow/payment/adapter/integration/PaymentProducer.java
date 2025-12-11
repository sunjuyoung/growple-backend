package com.grow.payment.adapter.integration;

import com.grow.common.PaymentEnrollmentEvent;
import com.grow.common.StudyCreateEvent;
import com.grow.payment.application.required.PaymentPublisher;
import io.swagger.v3.oas.models.media.JsonSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentProducer implements PaymentPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    //스터디 참여 결제
    @Override
    public void publishPaymentEnrolledEvent(PaymentEnrollmentEvent message){
        kafkaTemplate.send(Topics.PAYMENT_ENROLLED, message);
    }

    //스터디 생성 결제완료 -> RECRUITING
    @Override
    public void publishStudyCreatedEvent(StudyCreateEvent message){
        kafkaTemplate.send(Topics.STUDY_CREATED, message);
    }

}
