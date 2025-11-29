package com.grow.payment.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PendingPaymentEvent {
    private Long paymentEventId;
    private String paymentKey;
    private String orderId;
    private List<PendingPaymentOrder> pendingPaymentOrders;

    public PendingPaymentEvent(Long paymentEventId, String paymentKey, String orderId,
                              List<PendingPaymentOrder> pendingPaymentOrders) {
        this.paymentEventId = paymentEventId;
        this.paymentKey = paymentKey;
        this.orderId = orderId;
        this.pendingPaymentOrders = pendingPaymentOrders != null ?
                new ArrayList<>(pendingPaymentOrders) : new ArrayList<>();
    }

    public Long getPaymentEventId() {
        return paymentEventId;
    }

    public String getPaymentKey() {
        return paymentKey;
    }

    public String getOrderId() {
        return orderId;
    }

    public List<PendingPaymentOrder> getPendingPaymentOrders() {
        return Collections.unmodifiableList(pendingPaymentOrders);
    }

    public Long totalAmount() {
        return pendingPaymentOrders.stream()
                .mapToLong(PendingPaymentOrder::getAmount)
                .sum();
    }
}
