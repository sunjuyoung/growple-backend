//package com.example.payment_service.payment.application.service;
//
//import com.example.payment_service.payment.application.port.in.PaymentConfirmCommand;
//import com.example.payment_service.payment.application.port.in.PaymentConfirmUseCase;
//import com.example.payment_service.payment.application.port.out.PaymentExecutorPort;
//import com.example.payment_service.payment.application.port.out.PaymentStatusUpdateCommand;
//import com.example.payment_service.payment.application.port.out.PaymentStatusUpdatePort;
//import com.example.payment_service.payment.application.port.out.PaymentValidationPort;
//import com.example.payment_service.payment.domain.PaymentConfirmationResult;
//import com.example.payment_service.payment.domain.PaymentExecutionResult;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//@Service
//@RequiredArgsConstructor
//public class PaymentConfirmService implements PaymentConfirmUseCase {
//
//    private final PaymentStatusUpdatePort paymentStatusUpdatePort;
//    private final PaymentValidationPort paymentValidationPort;
//    private final PaymentExecutorPort paymentExecutorPort;
//    private final PaymentErrorHandler paymentErrorHandler;
//
//    @Override
//    @Transactional
//    public PaymentConfirmationResult confirm(PaymentConfirmCommand command) {
//        try {
//            boolean updated = paymentStatusUpdatePort.updatePaymentStatusToExecuting(command.getOrderId(), command.getPaymentKey());
//
//            if (!updated) {
//                throw new IllegalStateException("Failed to update payment status to executing");
//            }
//
//            boolean isValid = paymentValidationPort.isValid(command.getOrderId(), command.getAmount());
//
//            if (!isValid) {
//                throw new IllegalStateException("Payment validation failed");
//            }
//
//            PaymentExecutionResult executionResult = paymentExecutorPort.execute(command);
//
//            PaymentStatusUpdateCommand updateCommand = new PaymentStatusUpdateCommand(
//                    executionResult.getPaymentKey(),
//                    executionResult.getOrderId(),
//                    executionResult.paymentStatus(),
//                    executionResult.getExtraDetails(),
//                    executionResult.getFailure()
//            );
//
//            paymentStatusUpdatePort.updatePaymentStatus(updateCommand);
//
//            return new PaymentConfirmationResult(executionResult.paymentStatus(), executionResult.getFailure());
//        } catch (Exception e) {
//            return paymentErrorHandler.handlePaymentConfirmationError(e, command);
//        }
//    }
//}
