package com.grow.study.adapter.intergration;


import com.grow.common.StudyCreateEvent;
import com.grow.study.adapter.persistence.ProcessedEventRepository;
import com.grow.study.application.NonRetryableException;
import com.grow.study.application.provided.StudyRegister;
import com.grow.common.PaymentEnrollmentEvent;
import com.grow.study.domain.ProcessedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class StudyEventHandler {

    private final StudyRegister studyRegister;
    private final StudyProducer studyProducer;
    private final SlackNotifier slackNotifier;
    private final ProcessedEventRepository processedEventRepository;

    //참여 결제가 완료되었다 해당 스터디에 멤버로 등록
    @KafkaListener(
            topics = Topics.PAYMENT_ENROLLED,
            groupId = "study-service-group"
           // concurrency = "2"
    )
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2),
            dltTopicSuffix = ".dlt"
    )
    @Transactional
    public void handlePaymentEnroll(PaymentEnrollmentEvent event) {
        // 멱등성 체크: 이미 처리된 이벤트인지 확인
        if (processedEventRepository.existsById(event.eventId())) {
            log.info("이미 처리된 이벤트 (멱등성 보장): eventId={}", event.eventId());
            return;
        }

        try {
            studyRegister.enrollment(
                    event.studyId(),
                    event.userId(),
                    event.amount()
            );

            // 처리 완료된 이벤트 저장
            processedEventRepository.save(ProcessedEvent.of(event.eventId(), "PAYMENT_ENROLLED"));
            log.info("이벤트 처리 완료: eventId={}, studyId={}, userId={}", event.eventId(), event.studyId(), event.userId());

        } catch (NonRetryableException e) {
            log.warn("비즈니스 오류로 즉시 환불: eventId={}, reason={}", event.eventId(), e.getMessage());
            // 실패해도 이벤트 처리 완료로 기록 (환불 이벤트 발행했으므로)
            processedEventRepository.save(ProcessedEvent.of(event.eventId(), "PAYMENT_ENROLLED_FAILED"));
            studyProducer.publishStudyEnrolledFailedEvent(event, e.getMessage());
        } catch (Exception e) {
            slackNotifier.sendError("스터디 참여 등록 중 오류 발생: ", e.getMessage());
            throw e; // 재시도를 위해 예외 다시 던짐
        }
    }

    //생성 결제가 완료 -> RECRUITING 상태로 변경
    @KafkaListener(topics =
            Topics.STUDY_CREATED,
            groupId = "study-service-group"
            //concurrency = "2"
    )
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2),
            dltTopicSuffix = ".dlt"
    )
    public void handlePaymentStudy(StudyCreateEvent event) {
        try {
            studyRegister.changeStudyStatus(event.studyId(), event.userId());
        }catch (Exception e){
            slackNotifier.sendError(" 생성 결제가 완료 -> RECRUITING 상태로 변경 중 오류 발생: " , e.getMessage());
        }

    }

}
