package com.grow.payment.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grow.common.PaymentEnrollmentEvent;
import com.grow.common.StudyCreateEvent;
import com.grow.payment.adapter.config.TossPaymentProperties;
import com.grow.payment.adapter.persistence.PaymentJpaRepository;
import com.grow.payment.adapter.webapi.dto.TossWebhookPayload;
import com.grow.payment.application.dto.StudySummaryResponse;
import com.grow.payment.application.required.PaymentPublisher;
import com.grow.payment.application.required.StudyRestClient;
import com.grow.payment.domain.Payment;
import com.grow.payment.domain.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * 토스페이먼츠 웹훅 처리 서비스
 *
 * 웹훅을 통해 결제 상태를 동기화합니다.
 * - 결제 완료: PENDING → COMPLETED
 * - 결제 취소: COMPLETED → CANCELLED
 * - 결제 실패: PENDING → FAILED
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final PaymentJpaRepository paymentRepository;
    private final PaymentPublisher paymentPublisher;
    private final StudyRestClient studyRestClient;
    private final TossPaymentProperties tossPaymentProperties;
    private final ObjectMapper objectMapper;

    /**
     * 웹훅 페이로드 처리
     *
     * 이벤트 타입에 따라 결제 상태를 업데이트합니다.
     */
    @Transactional
    public void processWebhook(TossWebhookPayload payload) {
        if (payload.getData() == null) {
            log.warn("웹훅 데이터가 비어있음");
            return;
        }

        String orderId = payload.getData().getOrderId();
        String paymentKey = payload.getData().getPaymentKey();
        String status = payload.getData().getStatus();

        log.info("웹훅 처리 시작: orderId={}, status={}", orderId, status);

        // 결제 완료
        if (payload.isPaymentDone()) {
            handlePaymentDone(payload);
        }
        // 결제 취소
        else if (payload.isPaymentCanceled()) {
            handlePaymentCanceled(payload);
        }
        // 결제 실패
        else if (payload.isPaymentFailed()) {
            handlePaymentFailed(payload);
        }
        else {
            log.info("처리하지 않는 웹훅 이벤트: eventType={}, status={}",
                    payload.getEventType(), status);
        }
    }

    /**
     * 결제 완료 처리
     *
     * PENDING 상태인 결제를 COMPLETED로 변경하고 이벤트를 발행합니다.
     * 이미 COMPLETED 상태면 중복 처리 방지를 위해 스킵합니다.
     */
    private void handlePaymentDone(TossWebhookPayload payload) {
        String orderId = payload.getData().getOrderId();
        String paymentKey = payload.getData().getPaymentKey();
        String method = payload.getData().getMethod();

        Optional<Payment> optPayment = paymentRepository.findByOrderId(orderId);

        if (optPayment.isEmpty()) {
            log.warn("존재하지 않는 주문에 대한 웹훅: orderId={}", orderId);
            return;
        }

        Payment payment = optPayment.get();

        // 이미 완료된 결제는 스킵 (멱등성)
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            log.info("이미 완료된 결제 - 웹훅 스킵: orderId={}", orderId);
            return;
        }

        // PENDING 상태가 아니면 처리 불가
        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.warn("완료 처리할 수 없는 상태: orderId={}, currentStatus={}",
                    orderId, payment.getStatus());
            return;
        }

        // 결제 완료 처리
        payment.approve(paymentKey, method);
        paymentRepository.save(payment);

        log.info("웹훅으로 결제 완료 처리: orderId={}, paymentKey={}", orderId, paymentKey);

        // 이벤트 발행
        publishPaymentEvent(payment, paymentKey);
    }

    /**
     * 결제 취소 처리
     *
     * COMPLETED 상태인 결제를 CANCELLED로 변경합니다.
     */
    private void handlePaymentCanceled(TossWebhookPayload payload) {
        String orderId = payload.getData().getOrderId();

        Optional<Payment> optPayment = paymentRepository.findByOrderId(orderId);

        if (optPayment.isEmpty()) {
            log.warn("존재하지 않는 주문에 대한 취소 웹훅: orderId={}", orderId);
            return;
        }

        Payment payment = optPayment.get();

        // 이미 취소된 결제는 스킵
        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            log.info("이미 취소된 결제 - 웹훅 스킵: orderId={}", orderId);
            return;
        }

        // 완료 상태가 아니면 취소 불가
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            log.warn("취소할 수 없는 상태: orderId={}, currentStatus={}",
                    orderId, payment.getStatus());
            return;
        }

        payment.cancel();
        paymentRepository.save(payment);

        log.info("웹훅으로 결제 취소 처리: orderId={}", orderId);

        // TODO: 스터디 참가 취소 이벤트 발행 (필요시)
    }

    /**
     * 결제 실패 처리
     *
     * PENDING 상태인 결제를 FAILED로 변경합니다.
     */
    private void handlePaymentFailed(TossWebhookPayload payload) {
        String orderId = payload.getData().getOrderId();
        String status = payload.getData().getStatus();

        Optional<Payment> optPayment = paymentRepository.findByOrderId(orderId);

        if (optPayment.isEmpty()) {
            log.warn("존재하지 않는 주문에 대한 실패 웹훅: orderId={}", orderId);
            return;
        }

        Payment payment = optPayment.get();

        // 이미 실패 처리된 결제는 스킵
        if (payment.getStatus() == PaymentStatus.FAILED) {
            log.info("이미 실패 처리된 결제 - 웹훅 스킵: orderId={}", orderId);
            return;
        }

        // PENDING 상태가 아니면 실패 처리 불가
        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.warn("실패 처리할 수 없는 상태: orderId={}, currentStatus={}",
                    orderId, payment.getStatus());
            return;
        }

        String failReason = "ABORTED".equals(status) ? "결제 승인 실패" : "결제 유효기간 만료";
        payment.fail(failReason);
        paymentRepository.save(payment);

        log.info("웹훅으로 결제 실패 처리: orderId={}, reason={}", orderId, failReason);
    }

    /**
     * 결제 완료 이벤트 발행
     *
     * 스터디 상태에 따라 적절한 이벤트를 발행합니다.
     */
    private void publishPaymentEvent(Payment payment, String paymentKey) {
        try {
            StudySummaryResponse studySummary = studyRestClient.getMemberSummary(payment.getStudyId());

            if ("PENDING".equals(studySummary.status())) {
                // 스터디 생성 결제
                log.info("스터디 생성 이벤트 발행 (웹훅): studyId={}", payment.getStudyId());
                paymentPublisher.publishStudyCreatedEvent(
                        StudyCreateEvent.of(
                                payment.getMemberId(),
                                payment.getStudyId(),
                                payment.getOrderName(),
                                payment.getAmount()
                        )
                );
            } else if ("RECRUITING".equals(studySummary.status())) {
                // 스터디 참여 결제
                log.info("스터디 참여 이벤트 발행 (웹훅): studyId={}", payment.getStudyId());
                paymentPublisher.publishPaymentEnrolledEvent(
                        PaymentEnrollmentEvent.of(
                                payment.getMemberId(),
                                payment.getStudyId(),
                                payment.getOrderName(),
                                payment.getAmount(),
                                paymentKey
                        )
                );
            } else {
                log.warn("이벤트 발행 불가 - 스터디 상태: studyId={}, status={}",
                        payment.getStudyId(), studySummary.status());
            }
        } catch (Exception e) {
            // 이벤트 발행 실패는 로깅만 하고 웹훅 처리는 성공으로 처리
            // 이벤트 재발행은 별도 배치로 처리
            log.error("이벤트 발행 실패: orderId={}, error={}",
                    payment.getOrderId(), e.getMessage());
        }
    }

    /**
     * 웹훅 시그니처 검증
     *
     * 토스에서 전송한 시그니처를 검증하여 위조된 요청을 방지합니다.
     * HMAC-SHA256 알고리즘 사용
     *
     * @param signature Toss-Signature 헤더 값
     * @param payload 웹훅 페이로드
     * @throws SecurityException 시그니처 불일치 시
     */
    public void verifySignature(String signature, TossWebhookPayload payload) {
        if (signature == null || signature.isEmpty()) {
            log.warn("시그니처가 없는 웹훅 요청");
            throw new SecurityException("Missing webhook signature");
        }

        try {
            // 페이로드를 JSON 문자열로 변환
            String payloadJson = objectMapper.writeValueAsString(payload);

            // HMAC-SHA256으로 시그니처 생성
            String webhookSecretKey = tossPaymentProperties.getWebhookSecretKey();
            if (webhookSecretKey == null || webhookSecretKey.isEmpty()) {
                log.warn("웹훅 시크릿 키가 설정되지 않음 - 시그니처 검증 스킵");
                return;
            }
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    webhookSecretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(payloadJson.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getEncoder().encodeToString(hash);

            // 시그니처 비교
            if (!expectedSignature.equals(signature)) {
                log.warn("웹훅 시그니처 불일치: expected={}, actual={}",
                        expectedSignature, signature);
                throw new SecurityException("Invalid webhook signature");
            }

            log.debug("웹훅 시그니처 검증 성공");

        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            log.error("시그니처 검증 중 오류: {}", e.getMessage());
            throw new SecurityException("Signature verification failed", e);
        }
    }
}
