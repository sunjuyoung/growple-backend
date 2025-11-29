package com.grow.payment.application.service;

import com.grow.payment.application.port.in.CheckoutCommand;
import com.grow.payment.application.port.in.CheckoutUseCase;
import com.grow.payment.application.port.out.LoadProductPort;
import com.grow.payment.application.port.out.SavePaymentPort;
import com.grow.payment.domain.CheckoutResult;
import com.grow.payment.domain.PaymentEvent;
import com.grow.payment.domain.PaymentOrder;
import com.grow.payment.domain.Product;
import com.grow.payment.domain.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CheckoutService implements CheckoutUseCase {

    private final LoadProductPort loadProductPort;
    private final SavePaymentPort savePaymentPort;

    @Override
    @Transactional
    public CheckoutResult checkout(CheckoutCommand command) {
        List<Product> products = loadProductPort.getProducts(command.getCartId(), command.getProductIds());
        PaymentEvent paymentEvent = createPaymentEvent(command, products);
        savePaymentPort.save(paymentEvent);
        return new CheckoutResult(paymentEvent.totalAmount(), paymentEvent.getOrderId(), paymentEvent.getOrderName());
    }

    private PaymentEvent createPaymentEvent(CheckoutCommand command, List<Product> products) {
        String orderName = products.stream()
                .map(Product::getName)
                .collect(Collectors.joining(", "));

        List<PaymentOrder> paymentOrders = products.stream()
                .map(product -> PaymentOrder.builder()
                        .sellerId(product.getSellerId())
                        .orderId(command.getIdempotencyKey())
                        .productId(product.getId())
                        .amount(product.getAmount())
                        .paymentStatus(PaymentStatus.NOT_STARTED)
                        .isLedgerUpdated(false)
                        .isWalletUpdated(false)
                        .build())
                .collect(Collectors.toList());

        return PaymentEvent.builder()
                .buyerId(command.getBuyerId())
                .orderId(command.getIdempotencyKey())
                .orderName(orderName)
                .paymentOrders(paymentOrders)
                .isPaymentDone(false)
                .build();
    }
}
