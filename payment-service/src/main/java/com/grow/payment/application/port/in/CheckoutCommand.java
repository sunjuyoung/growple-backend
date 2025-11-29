package com.grow.payment.application.port.in;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutCommand {
    private Long cartId;
    private Long buyerId;
    private List<Long> productIds;
    private String idempotencyKey;
}
