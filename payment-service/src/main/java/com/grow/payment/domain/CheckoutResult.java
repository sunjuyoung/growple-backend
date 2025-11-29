package com.grow.payment.domain;

public class CheckoutResult {
    private  Long amount;
    private  String orderId;
    private  String orderName;

    public CheckoutResult(Long amount, String orderId, String orderName) {
        this.amount = amount;
        this.orderId = orderId;
        this.orderName = orderName;
    }

    public Long getAmount() {
        return amount;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getOrderName() {
        return orderName;
    }
}
