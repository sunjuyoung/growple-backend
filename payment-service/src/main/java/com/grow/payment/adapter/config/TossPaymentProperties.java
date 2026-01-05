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

    /**
     * 웹훅 시크릿 키 (시그니처 검증용)
     * 토스 대시보드 > 개발 정보 > 웹훅 설정에서 확인 가능
     */
    private String webhookSecretKey;
}
