package com.example.payment_service.payment.adapter.webapi;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CheckoutController {

    // checkout 페이지 렌더링
    @GetMapping("/")
    public String checkoutPage() {
        return "checkout";
    }

}
