package com.grow.payment.application;

import com.grow.common.StudyCreateEvent;
import com.grow.payment.adapter.integration.TossPaymentClient;
import com.grow.payment.adapter.integration.dto.TossConfirmResponse;
import com.grow.payment.adapter.persistence.PaymentJpaRepository;
import com.grow.payment.application.dto.*;
import com.grow.payment.application.provided.TossPayment;
import com.grow.payment.application.required.PaymentPublisher;
import com.grow.payment.application.required.StudyRestClient;
import com.grow.common.PaymentEnrollmentEvent;
import com.grow.payment.domain.AlreadyPaidException;
import com.grow.payment.domain.Payment;
import com.grow.payment.domain.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PaymentService implements TossPayment {

    private final PaymentJpaRepository paymentRepository;
    private final TossPaymentClient tossPaymentClient;
    private final PaymentPublisher paymentPublisher;
    private final StudyRestClient studyRestClient;

    /**
     * 결제 요청 (결제 생성)
     * - Payment 엔티티 생성 (status = PENDING)
     * - 프론트에서 토스 결제창 호출 시 필요한 정보 반환
     */
    @Override
    @Transactional
    public PaymentResponse requestPayment(PaymentRequestCommand command) {

        String orderId = generateOrderId(command.studyId(), command.memberId());

        Optional<Payment> existing = paymentRepository.findByOrderId(orderId);


        // 이미 결제 완료된 건이 있는지 확인
        if (existing.isPresent()) {
            Payment payment = existing.get();

            // 이미 결제 완료된 경우
            if (payment.getStatus() == PaymentStatus.COMPLETED) {
                throw new AlreadyPaidException("이미 결제가 완료된 스터디입니다.");
            }

            // PENDING 상태면 기존 주문 재사용 (멱등성)
            if (payment.getStatus() == PaymentStatus.PENDING) {
                return PaymentResponse.from(payment);
            }
        }


        Payment payment = Payment.create(
                command.memberId(),
                command.studyId(),
                command.orderName(),
                command.amount(),
                orderId
        );

        Payment saved = paymentRepository.save(payment);
        
        return PaymentResponse.from(saved);
    }

    private String generateOrderId(Long studyId, Long memberId) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return String.format("S%d_M%d_%s", studyId, memberId, uuid.substring(0, 12));
    }

    /**
     * 결제 승인
     * - 토스에서 redirect 후 호출
     * - paymentKey로 토스 승인 API 호출
     * - 성공 시 상태 변경 COMPLETED
     */
    @Override
    @Transactional
    public PaymentResponse confirmPayment(PaymentConfirmCommand command) {
        Payment payment = paymentRepository.findByOrderId(command.orderId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        // 금액 검증
        if (!payment.getAmount().equals(command.amount())) {
            payment.fail("금액이 일치하지 않습니다.");
            throw new IllegalArgumentException("결제 금액이 일치하지 않습니다.");
        }

        try {
            // 토스페이먼츠 승인 API 호출
            TossConfirmResponse tossResponse = tossPaymentClient.confirm(
                    command.paymentKey(), 
                    command.orderId(), 
                    command.amount()
            );

            // 승인 성공 처리
            payment.approve(command.paymentKey(), tossResponse.getPaymentMethod());
            log.info("결제 승인 완료: orderId={}, method={}", command.orderId(), tossResponse.getPaymentMethod());

            paymentRepository.save(payment);
        } catch (Exception e) {
            payment.fail(e.getMessage());
            log.error("결제 승인 실패: orderId={}, error={}", command.orderId(), e.getMessage());
            throw new IllegalStateException("결제 승인에 실패했습니다.");
        }

        //스터디 서비스 api
        StudySummaryResponse studySummary = studyRestClient.getMemberSummary(Long.valueOf(command.studyId()));


        //스터디 상태에 따른 처리
        //생성 결제
        if(studySummary.status().equals("PENDING")){
            //스터디 RECRUITING 상태로 변경 이벤트 발행
            log.info("스터디 생성 결제 완료 이벤트 발행: studyId={}", payment.getStudyId());
            paymentPublisher.publishStudyCreatedEvent(
                    StudyCreateEvent.of(
                            payment.getMemberId(),
                            payment.getStudyId(),
                            payment.getOrderName(),
                            payment.getAmount()
                    )
            );

            //참여 결제
        }else if (studySummary.status().equals("RECRUITING")){
            paymentPublisher.publishPaymentEnrolledEvent(PaymentEnrollmentEvent.of(
                    payment.getMemberId(),
                    payment.getStudyId(),
                    payment.getOrderName(),
                    payment.getAmount(),
                    command.paymentKey()
            ));

        }else {
            throw new IllegalStateException("해당 스터디는 현재 참여할 수 없는 상태입니다.");
        }


        return PaymentResponse.from(payment);
    }

    /**
     * 결제 취소 (스터디 시작 전)
     * - 테스트 환경: DB 상태만 변경
     * - 운영 환경: 토스 취소 API 호출
     */
    @Transactional
    public PaymentResponse cancelPayment(String orderId, String cancelReason) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        if (!payment.isCancellable()) {
            throw new IllegalStateException("취소할 수 없는 상태입니다.");
        }

        // 테스트 환경이므로 토스 API 호출 생략 (필요시 주석 해제)
        // tossPaymentClient.cancel(payment.getPaymentKey(), cancelReason);

        payment.cancel();
        log.info("결제 취소 완료: orderId={}", orderId);
        
        return PaymentResponse.from(payment);
    }

    /**
     * 결제 취소 (paymentKey 기반 - SAGA 보상 트랜잭션용)
     */
    @Transactional
    public PaymentResponse cancelByPaymentKey(String paymentKey, String cancelReason) {
        Payment payment = paymentRepository.findByPaymentKey(paymentKey)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결제입니다. paymentKey=" + paymentKey));

        if (!payment.isCancellable()) {
            throw new IllegalStateException("취소할 수 없는 상태입니다. status=" + payment.getStatus());
        }

        try {
            // 토스페이먼츠 취소 API 호출
            tossPaymentClient.cancel(paymentKey, cancelReason);
            payment.cancel();
            log.info("SAGA 보상 트랜잭션 - 결제 취소 완료: paymentKey={}, reason={}", paymentKey, cancelReason);
        } catch (Exception e) {
            log.error("SAGA 보상 트랜잭션 - 결제 취소 실패: paymentKey={}, error={}", paymentKey, e.getMessage());
            throw new IllegalStateException("결제 취소에 실패했습니다.", e);
        }

        return PaymentResponse.from(payment);
    }

    /**
     * 결제 조회
     */
    public PaymentResponse getPayment(String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));
        
        return PaymentResponse.from(payment);
    }

    /**
     * 특정 스터디의 완료된 결제 목록 조회 (정산용)
     */
    public List<PaymentResponse> getCompletedPaymentsByStudy(Long studyId) {
        return paymentRepository.findByStudyIdAndStatus(studyId, PaymentStatus.COMPLETED)
                .stream()
                .map(PaymentResponse::from)
                .toList();
    }

    /**
     * 회원의 결제 완료된 스터디 확인
     */
    public boolean hasCompletedPayment(Long memberId, Long studyId) {
        return paymentRepository.existsByMemberIdAndStudyIdAndStatus(memberId, studyId, PaymentStatus.COMPLETED);
    }
}
