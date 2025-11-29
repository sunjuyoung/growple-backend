//package com.example.payment_service.payment.application.service;
//
//import com.example.payment_service.payment.application.port.in.PaymentConfirmCommand;
//import com.example.payment_service.payment.application.port.in.PaymentRecoveryUseCase;
//import com.example.payment_service.payment.application.port.out.*;
//import com.example.payment_service.payment.domain.PendingPaymentEvent;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.annotation.Profile;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.concurrent.TimeUnit;
//
//@Slf4j
//@Service
//@Profile("dev")
//@RequiredArgsConstructor
//public class PaymentRecoveryService implements PaymentRecoveryUseCase {
//
//    private final LoadPendingPaymentPort loadPendingPaymentPort;
//    private final PaymentValidationPort paymentValidationPort;
//    private final PaymentExecutorPort paymentExecutorPort;
//    private final PaymentStatusUpdatePort paymentStatusUpdatePort;
//    private final PaymentErrorHandler paymentErrorHandler;
//
//    @Override
//    @Scheduled(fixedDelay = 180, initialDelay = 180, timeUnit = TimeUnit.SECONDS)
//    public void recovery() {
//        List<PendingPaymentEvent> pendingPayments = loadPendingPaymentPort.getPendingPayments();
//
//        pendingPayments.stream()
//                .map(event -> new PaymentConfirmCommand(
//                        event.getPaymentKey(),
//                        event.getOrderId(),
//                        event.totalAmount()))
//                .parallel()
//                .forEach(command -> {
//                    try {
//                        boolean isValid = paymentValidationPort.isValid(command.getOrderId(), command.getAmount());
//                        if (isValid) {
//                            PaymentExecutionResult executionResult = paymentExecutorPort.execute(command);
//                            paymentStatusUpdatePort.updatePaymentStatus(new PaymentStatusUpdateCommand(executionResult));
//                        }
//                    } catch (Exception e) {
//                        paymentErrorHandler.handlePaymentConfirmationError(e, command);
//                        log.error("Failed to recover payment for orderId: {}", command.getOrderId(), e);
//                    }
//                });
//    }
//}
