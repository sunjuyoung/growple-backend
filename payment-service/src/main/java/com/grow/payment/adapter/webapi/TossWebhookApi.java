package com.grow.payment.adapter.webapi;

import com.grow.payment.adapter.webapi.dto.TossWebhookPayload;
import com.grow.payment.application.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 토스페이먼츠 웹훅 수신 컨트롤러
 *
 * 토스에서 결제 상태 변경 시 호출하는 엔드포인트
 * - 결제 완료 (DONE)
 * - 결제 취소 (CANCELED)
 * - 결제 실패 (ABORTED, EXPIRED)
 */
@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class TossWebhookApi {

    private final WebhookService webhookService;

    /**
     * 토스페이먼츠 웹훅 수신
     *
     * POST /webhook/toss
     *
     * 토스 대시보드에서 웹훅 URL 설정 필요:
     * - 테스트: https://your-domain.com/webhook/toss
     * - 운영: https://your-domain.com/webhook/toss
     *
     * @param signature 토스에서 제공하는 웹훅 시그니처 (HMAC-SHA256)
     * @param payload 웹훅 페이로드
     * @return 200 OK (반드시 200을 반환해야 토스에서 재시도하지 않음)
     */
    @PostMapping("/toss")
    public ResponseEntity<String> handleTossWebhook(
            @RequestHeader(value = "Toss-Signature", required = false) String signature,
            @RequestBody TossWebhookPayload payload
    ) {
        log.info("토스 웹훅 수신: eventType={}, orderId={}, status={}",
                payload.getEventType(),
                payload.getData() != null ? payload.getData().getOrderId() : "null",
                payload.getData() != null ? payload.getData().getStatus() : "null"
        );

        try {
            // 시그니처 검증 (운영 환경 필수)
            // webhookService.verifySignature(signature, payload);

            // 웹훅 처리
            webhookService.processWebhook(payload);

            log.info("웹훅 처리 완료: orderId={}",
                    payload.getData() != null ? payload.getData().getOrderId() : "null");

            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            // 에러가 발생해도 200을 반환해야 토스에서 재시도하지 않음
            // 실패 건은 별도 모니터링/알림으로 처리
            log.error("웹훅 처리 실패: orderId={}, error={}",
                    payload.getData() != null ? payload.getData().getOrderId() : "null",
                    e.getMessage(), e);

            return ResponseEntity.ok("FAILED");
        }
    }

    /**
     * 웹훅 헬스체크
     *
     * 토스에서 웹훅 URL 등록 시 연결 테스트용
     */
    @GetMapping("/toss/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Webhook endpoint is healthy");
    }
}
