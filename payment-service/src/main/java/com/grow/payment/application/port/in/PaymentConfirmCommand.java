package com.grow.payment.application.port.in;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmCommand {
    private String paymentKey;
    private String orderId;
    private Long amount;
}
