package com.grow.payment.application;

import com.grow.payment.adapter.integration.TossPaymentClient;
import com.grow.payment.adapter.integration.dto.TossConfirmResponse;
import com.grow.payment.adapter.persistence.PaymentJpaRepository;
import com.grow.payment.application.dto.PaymentConfirmCommand;
import com.grow.payment.application.dto.PaymentRequestCommand;
import com.grow.payment.application.dto.PaymentResponse;
import com.grow.payment.domain.Payment;
import com.grow.payment.domain.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PaymentService {

    private final PaymentJpaRepository paymentRepository;
    private final TossPaymentClient tossPaymentClient;

    /**
     * 결제 요청 (주문 생성)
     * - Payment 엔티티 생성 (status = READY)
     * - 프론트에서 토스 결제창 호출 시 필요한 정보 반환
     */
    @Transactional
    public PaymentResponse requestPayment(PaymentRequestCommand command) {
        // 이미 결제 완료된 건이 있는지 확인
        boolean alreadyPaid = paymentRepository.existsByMemberIdAndStudyIdAndStatus(
                command.memberId(), 
                command.studyId(), 
                PaymentStatus.DONE
        );
        
        if (alreadyPaid) {
            throw new IllegalStateException("이미 결제가 완료된 스터디입니다.");
        }

        Payment payment = Payment.create(
                command.memberId(),
                command.studyId(),
                command.orderName(),
                command.amount()
        );

        Payment saved = paymentRepository.save(payment);
        log.info("결제 요청 생성: orderId={}, memberId={}, studyId={}", 
                saved.getOrderId(), command.memberId(), command.studyId());
        
        return PaymentResponse.from(saved);
    }

    /**
     * 결제 승인
     * - 토스에서 redirect 후 호출
     * - paymentKey로 토스 승인 API 호출
     * - 성공 시 상태 변경
     */
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
            
        } catch (Exception e) {
            payment.fail(e.getMessage());
            log.error("결제 승인 실패: orderId={}, error={}", command.orderId(), e.getMessage());
            throw e;
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
        return paymentRepository.findByStudyIdAndStatus(studyId, PaymentStatus.DONE)
                .stream()
                .map(PaymentResponse::from)
                .toList();
    }

    /**
     * 회원의 결제 완료된 스터디 확인
     */
    public boolean hasCompletedPayment(Long memberId, Long studyId) {
        return paymentRepository.existsByMemberIdAndStudyIdAndStatus(memberId, studyId, PaymentStatus.DONE);
    }
}
