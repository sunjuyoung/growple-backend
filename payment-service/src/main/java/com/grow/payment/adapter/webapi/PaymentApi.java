package com.grow.payment.adapter.webapi;

import com.grow.payment.adapter.webapi.dto.PaymentCancelRequest;
import com.grow.payment.adapter.webapi.dto.PaymentConfirmRequest;
import com.grow.payment.adapter.webapi.dto.PaymentRequest;
import com.grow.payment.application.PaymentService;
import com.grow.payment.application.dto.PaymentConfirmCommand;
import com.grow.payment.application.dto.PaymentRequestCommand;
import com.grow.payment.application.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentApi {

    private final PaymentService paymentService;

    /**
     * 결제 요청 (주문 생성)
     * POST /api/payments
     * 
     * 프론트에서 스터디 참여 버튼 클릭 시 호출
     * → orderId, amount 등 반환
     * → 프론트에서 토스 SDK로 결제창 호출
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> requestPayment(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody PaymentRequest request
    ) {
        PaymentRequestCommand command = PaymentRequestCommand.of(
                userId,
                request.studyId(),
                request.orderName(),
                request.amount()
        );
        
        PaymentResponse response = paymentService.requestPayment(command);
        return ResponseEntity.ok(response);
    }

    /**
     * 결제 승인
     * POST /api/payments/confirm
     * 
     * 토스 결제창 완료 후 redirect 시 호출
     * → 토스 승인 API 호출 후 결제 완료 처리
     */
    @PostMapping("/confirm")
    public ResponseEntity<PaymentResponse> confirmPayment(
            @RequestBody PaymentConfirmRequest request
    ) {
        PaymentConfirmCommand command = PaymentConfirmCommand.of(
                request.paymentKey(),
                request.orderId(),
                request.amount(),
                request.studyId()
        );
        
        PaymentResponse response = paymentService.confirmPayment(command);
        return ResponseEntity.ok(response);
    }

    /**
     * 결제 취소 (스터디 시작 전)
     * POST /api/payments/{orderId}/cancel
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<PaymentResponse> cancelPayment(
            @PathVariable String orderId,
            @RequestBody(required = false) PaymentCancelRequest request
    ) {
        String cancelReason = request != null ? request.cancelReason() : "사용자 요청";
        PaymentResponse response = paymentService.cancelPayment(orderId, cancelReason);
        return ResponseEntity.ok(response);
    }

    /**
     * 결제 조회
     * GET /api/payments/{orderId}
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable String orderId
    ) {
        PaymentResponse response = paymentService.getPayment(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * 스터디별 완료된 결제 목록 (정산용 - 내부 API)
     * GET /api/payments/study/{studyId}/completed
     */
    @GetMapping("/study/{studyId}/completed")
    public ResponseEntity<List<PaymentResponse>> getCompletedPaymentsByStudy(
            @PathVariable Long studyId
    ) {
        List<PaymentResponse> responses = paymentService.getCompletedPaymentsByStudy(studyId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 결제 완료 여부 확인 (내부 API)
     * GET /api/payments/check?memberId={}&studyId={}
     */
    @GetMapping("/check")
    public ResponseEntity<Boolean> checkPaymentCompleted(
            @RequestParam Long memberId,
            @RequestParam Long studyId
    ) {
        boolean completed = paymentService.hasCompletedPayment(memberId, studyId);
        return ResponseEntity.ok(completed);
    }
}
