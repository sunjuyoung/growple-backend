package com.grow.study.adapter.intergration;


import com.grow.study.application.NonRetryableException;
import com.grow.study.application.provided.StudyRegister;
import com.grow.common.PaymentEnrollmentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StudyEventHandler {

    private final StudyRegister studyRegister;
    private final StudyProducer studyProducer;

    //참여 결제가 완료되었다 해당 스터디에 멤버로 등록
    @KafkaListener(topics = Topcis.PAYMENT_ENROLLED, groupId = "study-service-group")
    public void handlePaymentFailed(PaymentEnrollmentEvent event) {
        // Compensation: cancel the order on payment failure

        try {
            studyRegister.enrollment(
                    event.studyId(),
                    event.userId(),
                    event.amount()
            );
        }catch (NonRetryableException e){
            log.warn("비즈니스 오류로 즉시 환불: {}", e.getMessage());
            studyProducer.publishStudyEnrolledFailedEvent(event, e.getMessage());
        }

    }

}
