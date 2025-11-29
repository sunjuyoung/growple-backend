package com.grow.payment.domain;

import com.grow.payment.domain.enums.PaymentMethod;
import com.grow.payment.domain.enums.PaymentStatus;
import com.grow.payment.domain.enums.PaymentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payment_events")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "order_name", nullable = false)
    private String orderName;

    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    @Column(name = "payment_key")
    private String paymentKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type")
    private PaymentType paymentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_event_id")
    @Builder.Default
    private List<PaymentOrder> paymentOrders = new ArrayList<>();

    @Column(name = "is_payment_done", nullable = false)
    private boolean isPaymentDone;

    public Long totalAmount() {
        return paymentOrders.stream()
                .mapToLong(PaymentOrder::getAmount)
                .sum();
    }

    public boolean isSuccess() {
        return paymentOrders.stream()
                .allMatch(order -> order.getPaymentStatus() == PaymentStatus.SUCCESS);
    }

    public boolean isFailure() {
        return paymentOrders.stream()
                .allMatch(order -> order.getPaymentStatus() == PaymentStatus.FAILURE);
    }

    public boolean isUnknown() {
        return paymentOrders.stream()
                .allMatch(order -> order.getPaymentStatus() == PaymentStatus.UNKNOWN);
    }

    public void confirmWalletUpdate() {
        paymentOrders.forEach(PaymentOrder::confirmWalletUpdate);
    }

    public void confirmLedgerUpdate() {
        paymentOrders.forEach(PaymentOrder::confirmLedgerUpdate);
    }

    public void completeIfDone() {
        if (allPaymentOrdersDone()) {
            this.isPaymentDone = true;
        }
    }

    public boolean isLedgerUpdateDone() {
        return paymentOrders.stream()
                .allMatch(PaymentOrder::isLedgerUpdated);
    }

    public boolean isWalletUpdateDone() {
        return paymentOrders.stream()
                .allMatch(PaymentOrder::isWalletUpdated);
    }

    private boolean allPaymentOrdersDone() {
        return paymentOrders.stream()
                .allMatch(order -> order.isWalletUpdated() && order.isLedgerUpdated());
    }
}
