//package com.example.payment_service.payment.application.service;
//
//import com.example.payment_service.payment.application.port.in.PaymentConfirmCommand;
//import com.example.payment_service.payment.application.port.out.PaymentStatusUpdateCommand;
//import com.example.payment_service.payment.application.port.out.PaymentStatusUpdatePort;
//import com.example.payment_service.payment.domain.PaymentConfirmationResult;
//import com.example.payment_service.payment.domain.PaymentFailure;
//import com.example.payment_service.payment.domain.enums.PaymentStatus;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.util.concurrent.TimeoutException;
//
//@Service
//@RequiredArgsConstructor
//public class PaymentErrorHandler {
//
//    private final PaymentStatusUpdatePort paymentStatusUpdatePort;
//
//    public PaymentConfirmationResult handlePaymentConfirmationError(Throwable error, PaymentConfirmCommand command) {
//        PaymentStatus status;
//        PaymentFailure failure;
//
//        // 기본적으로 UNKNOWN 상태로 처리
//        if (error instanceof TimeoutException) {
//            status = PaymentStatus.UNKNOWN;
//            failure = new PaymentFailure(
//                    error.getClass().getSimpleName(),
//                    error.getMessage() != null ? error.getMessage() : "Timeout occurred"
//            );
//        } else if (error instanceof IllegalStateException) {
//            status = PaymentStatus.FAILURE;
//            failure = new PaymentFailure(
//                    error.getClass().getSimpleName(),
//                    error.getMessage() != null ? error.getMessage() : "Validation failed"
//            );
//        } else {
//            status = PaymentStatus.UNKNOWN;
//            failure = new PaymentFailure(
//                    error.getClass().getSimpleName(),
//                    error.getMessage() != null ? error.getMessage() : "Unknown error"
//            );
//        }
//
//        PaymentStatusUpdateCommand updateCommand = new PaymentStatusUpdateCommand(
//                command.getPaymentKey(),
//                command.getOrderId(),
//                status,
//                null,
//                failure
//        );
//
//        paymentStatusUpdatePort.updatePaymentStatus(updateCommand);
//
//        return new PaymentConfirmationResult(status, failure);
//    }
//}
