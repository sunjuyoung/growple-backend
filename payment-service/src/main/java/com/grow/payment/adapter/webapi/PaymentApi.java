package com.example.payment_service.payment.adapter.webapi;

import com.example.payment_service.payment.adapter.webapi.dto.TossPaymentConfirmRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/toss")
public class PaymentApi {


    @PostMapping("/confirm")
    public String confirm(@RequestBody TossPaymentConfirmRequest request){

    }



    @GetMapping("/success")
    public String paymentSuccess() {
        return "Payment Successful!";
    }

    @GetMapping("/fail")
    public String paymentFailure() {
        return "Payment Failed!";
    }
}
