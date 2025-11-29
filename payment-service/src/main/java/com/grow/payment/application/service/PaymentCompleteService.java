//package com.example.payment_service.payment.application.service;
//
//import com.example.payment_service.payment.application.port.in.PaymentCompleteUseCase;
//import com.example.payment_service.payment.application.port.out.CompletePaymentPort;
//import com.example.payment_service.payment.application.port.out.LoadPaymentPort;
//import com.example.payment_service.payment.domain.LedgerEventMessage;
//import com.example.payment_service.payment.domain.PaymentEvent;
//import com.example.payment_service.payment.domain.WalletEventMessage;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//@Service
//@RequiredArgsConstructor
//public class PaymentCompleteService implements PaymentCompleteUseCase {
//
//    private final LoadPaymentPort loadPaymentPort;
//    private final CompletePaymentPort completePaymentPort;
//
//    @Override
//    @Transactional
//    public void completePayment(WalletEventMessage walletEventMessage) {
//        PaymentEvent paymentEvent = loadPaymentPort.getPayment(walletEventMessage.orderId());
//        paymentEvent.confirmWalletUpdate();
//        paymentEvent.completeIfDone();
//        completePaymentPort.complete(paymentEvent);
//    }
//
//    @Override
//    @Transactional
//    public void completePayment(LedgerEventMessage ledgerEventMessage) {
//        PaymentEvent paymentEvent = loadPaymentPort.getPayment(ledgerEventMessage.orderId());
//        paymentEvent.confirmLedgerUpdate();
//        paymentEvent.completeIfDone();
//        completePaymentPort.complete(paymentEvent);
//    }
//}
