package com.grow.payment.application.required;

/**
 * 회원 서비스 REST 클라이언트 포트 (Output Port)
 * 포인트 지급 등 회원 서비스와의 통신에 사용
 */
public interface MemberRestClient {

    /**
     * 포인트 지급 (정산 환급)
     *
     * @param memberId 회원 ID
     * @param amount   지급 금액
     * @param reason   지급 사유
     * @return 생성된 포인트 트랜잭션 ID (nullable)
     */
    Long addPoints(Long memberId, long amount, String reason);

    /**
     * 회원 존재 여부 확인
     */
    boolean existsMember(Long memberId);
}
