package com.grow.payment.adapter.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;

/**
 * 토스 결제 승인/취소 응답
 * - 필요한 필드만 매핑 (나머지는 무시)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TossConfirmResponse(
        String paymentKey,
        String orderId,
        String orderName,
        String status,          // DONE, CANCELED, etc.
        String method,          // 카드, 가상계좌, 간편결제 등
        Integer totalAmount,
        Integer balanceAmount,  // 취소 가능 금액
        String approvedAt,      // 결제 승인 시각
        String requestedAt,     // 결제 요청 시각
        Card card,              // 카드 결제 시
        EasyPay easyPay         // 간편결제 시
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Card(
            String company,         // 카드사
            String number,          // 마스킹된 카드번호
            String cardType,        // 신용, 체크 등
            String ownerType,       // 개인, 법인
            String approveNo,       // 승인번호
            Integer installmentPlanMonths  // 할부 개월
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EasyPay(
            String provider,        // 토스페이, 카카오페이 등
            Integer amount,
            Integer discountAmount
    ) {}
    
    /**
     * 결제 수단 문자열 반환
     */
    public String getPaymentMethod() {
        if (easyPay != null && easyPay.provider() != null) {
            return easyPay.provider();
        }
        if (card != null && card.company() != null) {
            return card.company();
        }
        return method;
    }
}
