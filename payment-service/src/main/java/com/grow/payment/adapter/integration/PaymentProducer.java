package com.grow.payment.adapter.integration;

import com.grow.common.PaymentEnrollmentEvent;
import com.grow.payment.application.required.PaymentPublisher;
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

}
