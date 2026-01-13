package com.grow.payment.adapter.integration;

import com.grow.common.RefundRequestEvent;
import com.grow.payment.adapter.persistence.ProcessedEventRepository;
import com.grow.payment.application.PaymentService;
import com.grow.payment.domain.ProcessedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventHandler {

    private final PaymentService paymentService;
    private final ProcessedEventRepository processedEventRepository;

    /**
     * 스터디 등록 실패 이벤트 처리 (SAGA 보상 트랜잭션)
     * - study-service에서 멤버 등록 실패 시 발행
     * - 결제 취소(환불) 처리
     */
    @KafkaListener(
            topics = Topics.STUDY_ENROLLMENT_FAILED,
            groupId = "payment-service-group"
    )
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2),
            dltTopicSuffix = ".dlt"
    )
    @Transactional
    public void handleStudyEnrollmentFailed(RefundRequestEvent event) {
        // 멱등성 체크: 이미 처리된 이벤트인지 확인
        if (processedEventRepository.existsById(event.eventId())) {
            log.info("이미 처리된 환불 이벤트 (멱등성 보장): eventId={}", event.eventId());
            return;
        }

        log.info("SAGA 보상 트랜잭션 시작 - 스터디 등록 실패로 환불 처리: eventId={}, userId={}, studyId={}, paymentKey={}, reason={}",
                event.eventId(), event.userId(), event.studyId(), event.paymentKey(), event.reason());

        try {
            paymentService.cancelByPaymentKey(event.paymentKey(), event.reason());

            // 처리 완료된 이벤트 저장
            processedEventRepository.save(ProcessedEvent.of(event.eventId(), "STUDY_ENROLLMENT_FAILED"));
            log.info("SAGA 보상 트랜잭션 완료 - 환불 처리 성공: eventId={}, paymentKey={}", event.eventId(), event.paymentKey());

        } catch (Exception e) {
            log.error("SAGA 보상 트랜잭션 실패 - 환불 처리 실패: eventId={}, paymentKey={}, error={}",
                    event.eventId(), event.paymentKey(), e.getMessage());
            throw e; // RetryableTopic에 의해 재시도
        }
    }
}
