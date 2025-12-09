package com.grow.payment.domain;

import com.grow.payment.domain.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends AbstractEntity {

    // === 연관 ID (MSA - 엔티티 참조 X) ===
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "study_id", nullable = false)
    private Long studyId;

    // === 주문 정보 ===
    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    @Column(name = "order_name", nullable = false)
    private String orderName;

    // === 금액 ===
    @Column(nullable = false)
    private Integer amount;

    // === 토스 응답 정보 ===
    @Column(name = "payment_key")
    private String paymentKey;

    @Column(name = "method")
    private String method;  // 카드, 토스페이, 카카오페이 등

    // === 상태 ===
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    // === 시간 ===
    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    // === 실패 정보 ===
    @Column(name = "fail_reason")
    private String failReason;

    // === 생성 메서드 ===
    @Builder
    private Payment(Long memberId, Long studyId, String orderName, Integer amount) {
        this.memberId = memberId;
        this.studyId = studyId;
        this.orderId = generateOrderId();
        this.orderName = orderName;
        this.amount = amount;
        this.status = PaymentStatus.READY;
        this.requestedAt = LocalDateTime.now();
    }

    public static Payment create(Long memberId, Long studyId, String orderName, Integer amount) {
        return Payment.builder()
                .memberId(memberId)
                .studyId(studyId)
                .orderName(orderName)
                .amount(amount)
                .build();
    }

    // === 비즈니스 메서드 ===
    public void approve(String paymentKey, String method) {
        this.paymentKey = paymentKey;
        this.method = method;
        this.status = PaymentStatus.DONE;
        this.approvedAt = LocalDateTime.now();
    }

    public void fail(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failReason = reason;
    }

    public void cancel() {
        this.status = PaymentStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }

    public boolean isDone() {
        return this.status == PaymentStatus.DONE;
    }

    public boolean isCancellable() {
        return this.status == PaymentStatus.DONE;
    }

    // === orderId 생성 ===
    private static String generateOrderId() {
        return "ORD_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
