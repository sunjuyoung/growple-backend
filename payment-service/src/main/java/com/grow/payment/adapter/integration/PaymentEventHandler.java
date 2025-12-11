package com.grow.payment.adapter.integration;

import com.grow.payment.application.dto.PaymentRequestCommand;
import com.grow.common.StudyCreateEvent;
import com.grow.payment.application.provided.TossPayment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventHandler {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TossPayment tossPayment;

//    @KafkaListener(topics = Topics.STUDY_CREATED, groupId = "payment-service-group")
//    public void processStudyCreatedEvent(StudyCreateEvent event) {
//        // Handle the study created event
//        log.info("Received study created event: {}", event);
//        // Add your business logic here
//        tossPayment.requestPayment(
//                PaymentRequestCommand.of(event.userId(), event.studyId(), event.orderName(), event.amount())
//        );
//    }
}
