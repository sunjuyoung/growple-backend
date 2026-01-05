package com.grow.payment.adapter.integration;

import com.grow.payment.adapter.integration.dto.PointRefundRequest;
import com.grow.payment.adapter.integration.dto.PointRefundResponse;
import com.grow.payment.application.required.MemberRestClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class MemberRestClientAdapter implements MemberRestClient {

    private final RestClient restClient;

    public MemberRestClientAdapter(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    /**
     * 포인트 지급 (정산 환급)
     * Member Service의 Internal API 호출
     */
    @Override
    public Long addPoints(Long memberId, long amount, String reason) {
        log.info("포인트 지급 요청 - memberId: {}, amount: {}, reason: {}", memberId, amount, reason);

        try {
            PointRefundRequest request = new PointRefundRequest(
                    (int) amount,
                    null,  // settlementItemId는 호출 측에서 관리
                    reason
            );

            ResponseEntity<PointRefundResponse> response = restClient.post()
                    .uri("http://member-service/internal/members/{memberId}/refund", memberId)
                    .body(request)
                    .retrieve()
                    .toEntity(PointRefundResponse.class);

            if (response.getBody() != null && response.getBody().success()) {
                log.info("포인트 지급 완료 - memberId: {}, amount: {}", memberId, amount);
                return response.getBody().settlementItemId();
            } else {
                String errorMsg = response.getBody() != null ? response.getBody().message() : "Unknown error";
                log.error("포인트 지급 실패 - memberId: {}, error: {}", memberId, errorMsg);
                throw new RuntimeException("포인트 지급 실패: " + errorMsg);
            }
        } catch (RestClientException e) {
            log.error("포인트 지급 API 호출 실패 - memberId: {}, error: {}", memberId, e.getMessage());
            throw new RuntimeException("포인트 지급 API 호출 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 회원 존재 여부 확인
     */
    @Override
    public boolean existsMember(Long memberId) {
        try {
            ResponseEntity<Integer> response = restClient.get()
                    .uri("http://member-service/internal/members/{memberId}/point", memberId)
                    .retrieve()
                    .toEntity(Integer.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            log.warn("회원 존재 확인 실패 - memberId: {}", memberId);
            return false;
        }
    }
}
