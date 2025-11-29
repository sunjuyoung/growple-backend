//package com.example.payment_service.payment.application.service;
//
//import com.example.payment_service.payment.application.port.in.PaymentEventMessageRelayUseCase;
//import com.example.payment_service.payment.application.port.out.DispatchEventMessagePort;
//import com.example.payment_service.payment.application.port.out.LoadPendingPaymentEventMessagePort;
//import com.example.payment_service.payment.domain.PaymentEventMessage;
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
//public class PaymentEventMessageRelayService implements PaymentEventMessageRelayUseCase {
//
//    private final LoadPendingPaymentEventMessagePort loadPendingPaymentEventMessagePort;
//    private final DispatchEventMessagePort dispatchEventMessagePort;
//
//    @Override
//    @Scheduled(fixedDelay = 180, initialDelay = 180, timeUnit = TimeUnit.SECONDS)
//    public void relay() {
//        List<PaymentEventMessage> pendingMessages = loadPendingPaymentEventMessagePort.getPendingPaymentEventMessage();
//
//        pendingMessages.forEach(message -> {
//            try {
//                dispatchEventMessagePort.dispatch(message);
//            } catch (Exception e) {
//                log.error("Failed to relay message", e);
//            }
//        });
//    }
//}
