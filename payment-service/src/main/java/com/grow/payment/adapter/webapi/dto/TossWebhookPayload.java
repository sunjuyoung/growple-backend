package com.grow.payment.adapter.webapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.OffsetDateTime;

/**
 * 토스페이먼츠 웹훅 페이로드
 *
 * 토스에서 결제 상태 변경 시 전송하는 웹훅 데이터
 * https://docs.tosspayments.com/reference/webhook
 */
@Getter
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class TossWebhookPayload {

    /**
     * 이벤트 타입
     * - PAYMENT_STATUS_CHANGED: 결제 상태 변경
     * - PAYOUT_STATUS_CHANGED: 정산 상태 변경
     * - DEPOSIT_CALLBACK: 가상계좌 입금
     */
    private String eventType;

    /**
     * 생성 시간 (ISO 8601)
     */
    private String createdAt;

    /**
     * 결제 데이터
     */
    private PaymentData data;

    @Getter
    @NoArgsConstructor
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentData {

        /**
         * 토스 결제 키
         */
        private String paymentKey;

        /**
         * 주문 ID (우리 시스템에서 생성한 ID)
         */
        private String orderId;

        /**
         * 결제 상태
         * - READY: 결제 준비
         * - IN_PROGRESS: 결제 진행 중
         * - WAITING_FOR_DEPOSIT: 가상계좌 입금 대기
         * - DONE: 결제 완료
         * - CANCELED: 결제 취소
         * - PARTIAL_CANCELED: 부분 취소
         * - ABORTED: 결제 승인 실패
         * - EXPIRED: 유효 기간 만료
         */
        private String status;

        /**
         * 주문명
         */
        private String orderName;

        /**
         * 결제 금액
         */
        private Integer totalAmount;

        /**
         * 결제 수단 (카드, 가상계좌, 간편결제 등)
         */
        private String method;

        /**
         * 결제 승인 시간
         */
        private String approvedAt;

        /**
         * 취소 정보 (취소 시에만 존재)
         */
        private CancelData[] cancels;
    }

    @Getter
    @NoArgsConstructor
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CancelData {
        private Integer cancelAmount;
        private String cancelReason;
        private String canceledAt;
        private String transactionKey;
    }

    /**
     * 결제 완료 이벤트인지 확인
     */
    public boolean isPaymentDone() {
        return "PAYMENT_STATUS_CHANGED".equals(eventType)
                && data != null
                && "DONE".equals(data.getStatus());
    }

    /**
     * 결제 취소 이벤트인지 확인
     */
    public boolean isPaymentCanceled() {
        return "PAYMENT_STATUS_CHANGED".equals(eventType)
                && data != null
                && ("CANCELED".equals(data.getStatus()) || "PARTIAL_CANCELED".equals(data.getStatus()));
    }

    /**
     * 결제 실패 이벤트인지 확인
     */
    public boolean isPaymentFailed() {
        return "PAYMENT_STATUS_CHANGED".equals(eventType)
                && data != null
                && ("ABORTED".equals(data.getStatus()) || "EXPIRED".equals(data.getStatus()));
    }
}
