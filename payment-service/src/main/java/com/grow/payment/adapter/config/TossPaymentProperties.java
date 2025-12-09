package com.grow.payment.adapter.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "psp.toss")
@Getter
@Setter
public class TossPaymentProperties {
    
    private String clientKey;
    private String secretKey;
    private String url;
}
