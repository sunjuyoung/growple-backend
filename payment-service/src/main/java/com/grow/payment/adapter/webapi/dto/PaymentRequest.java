package com.grow.payment.adapter.webapi.dto;

/**
 * 결제 요청 (프론트 → 백엔드)
 */
public record PaymentRequest(
        Long studyId,
        String orderName,   // "자바 스터디 참여비"
        Integer amount      // 10000
) {}
