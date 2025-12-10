package com.grow.payment.application.provided;

import com.grow.payment.application.dto.PaymentConfirmCommand;
import com.grow.payment.application.dto.PaymentRequestCommand;
import com.grow.payment.application.dto.PaymentResponse;

public interface TossPayment {

     PaymentResponse requestPayment(PaymentRequestCommand command);

     PaymentResponse confirmPayment(PaymentConfirmCommand command) ;
}
