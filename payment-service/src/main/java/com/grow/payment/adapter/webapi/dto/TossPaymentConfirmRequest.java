package com.example.payment_service.payment.adapter.webapi.dto;

public record TossPaymentConfirmRequest(
    String orderId,
    String paymentKey,
    Long amount
) {
}
