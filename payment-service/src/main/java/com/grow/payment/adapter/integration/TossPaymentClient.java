package com.grow.payment.adapter.integration;

import com.grow.payment.adapter.integration.dto.TossConfirmRequest;
import com.grow.payment.adapter.integration.dto.TossConfirmResponse;
import com.grow.payment.adapter.integration.dto.TossCancelRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

@Component
@RequiredArgsConstructor
@Slf4j
public class TossPaymentClient {

    private final RestClient tossRestClient;

    @Value("${PSP.toss.secretKey}")
    private String secretKey;


    /**
     * 결제 승인 API
     * POST /v1/payments/confirm
     */
    public TossConfirmResponse confirm(String paymentKey, String orderId, Integer amount) throws UnsupportedEncodingException {
        TossConfirmRequest request = new TossConfirmRequest(paymentKey, orderId, amount);

        Base64.Encoder encoder = Base64.getEncoder();
        // 비밀번호가 없다는 것을 알리기 위해 시크릿 키 뒤에 콜론을 추가합니다.
        byte[] encodedBytes = encoder.encode((secretKey + ":").getBytes("UTF-8"));
        String encodedSecretKey = "Basic " + new String(encodedBytes, 0, encodedBytes.length);


        log.info("토스 결제 승인 요청: orderId={}, amount={}", orderId, amount);

        TossConfirmResponse response = tossRestClient.post()
                .uri("/v1/payments/confirm")
                .header(HttpHeaders.AUTHORIZATION, encodedSecretKey)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .header("Idempotency-Key", orderId)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    log.error("토스 결제 승인 실패: status={}", res.getStatusCode());
                    throw new TossPaymentException("결제 승인 실패: " + res.getStatusCode());
                })
                .body(TossConfirmResponse.class);

        log.info("토스 결제 승인 성공: orderId={}, paymentKey={}", orderId, paymentKey);
        return response;
    }

    /**
     * 결제 취소 API
     * POST /v1/payments/{paymentKey}/cancel
     */
    public TossConfirmResponse cancel(String paymentKey, String cancelReason) {
        TossCancelRequest request = new TossCancelRequest(cancelReason);

        log.info("토스 결제 취소 요청: paymentKey={}, reason={}", paymentKey, cancelReason);

        TossConfirmResponse response = tossRestClient.post()
                .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    log.error("토스 결제 취소 실패: status={}", res.getStatusCode());
                    throw new TossPaymentException("결제 취소 실패: " + res.getStatusCode());
                })
                .body(TossConfirmResponse.class);

        log.info("토스 결제 취소 성공: paymentKey={}", paymentKey);
        return response;
    }
}
