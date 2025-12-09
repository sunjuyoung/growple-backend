package com.grow.payment.domain.enums;

/**
 * 결제 상태
 * 토스페이먼츠 상태와 매핑
 */
public enum PaymentStatus {
    READY("결제 준비"),           // 주문 생성됨, 결제 대기
    DONE("결제 완료"),            // 승인 완료
    CANCELLED("결제 취소"),       // 스터디 시작 전 취소
    FAILED("결제 실패");          // 결제 실패

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
