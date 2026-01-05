# 토스페이먼츠 웹훅 구현 가이드

## 1. 개요

### 웹훅이란?
> 토스페이먼츠에서 결제 상태가 변경될 때 우리 서버로 **실시간 알림**을 보내주는 기능

### 왜 필요한가?
```
[문제 상황]
사용자가 결제 완료 → 네트워크 문제로 redirect 실패
→ 토스에서는 DONE, 우리 시스템은 PENDING (불일치!)

[웹훅 도입 후]
토스에서 웹훅으로 DONE 알림 → 우리 시스템도 COMPLETED로 동기화
→ redirect 성공/실패와 무관하게 상태 일관성 보장
```

---

## 2. 구현 구조

```
[웹훅 플로우]

토스페이먼츠
    │
    ├─ 결제 완료 (DONE)
    ├─ 결제 취소 (CANCELED)
    └─ 결제 실패 (ABORTED/EXPIRED)
    │
    ▼
POST /webhook/toss
    │
    ├─ TossWebhookApi (컨트롤러)
    │      └─ 요청 수신 및 로깅
    │
    ├─ WebhookService (서비스)
    │      ├─ 시그니처 검증 (선택)
    │      ├─ 결제 상태 업데이트
    │      └─ 이벤트 발행
    │
    └─ 200 OK 응답 (필수!)
```

---

## 3. 파일 구조

```
payment-service/
├── adapter/webapi/
│   ├── TossWebhookApi.java           # 웹훅 컨트롤러
│   └── dto/
│       └── TossWebhookPayload.java   # 웹훅 페이로드 DTO
├── application/
│   └── WebhookService.java           # 웹훅 처리 로직
└── adapter/config/
    └── TossPaymentProperties.java    # 웹훅 시크릿 키 설정
```

---

## 4. 핵심 코드 설명

### 4.1 웹훅 페이로드 (TossWebhookPayload.java)

```java
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TossWebhookPayload {

    private String eventType;  // PAYMENT_STATUS_CHANGED
    private String createdAt;
    private PaymentData data;

    @Getter
    public static class PaymentData {
        private String paymentKey;
        private String orderId;
        private String status;      // DONE, CANCELED, ABORTED, EXPIRED
        private String method;      // 카드, 간편결제 등
        private Integer totalAmount;
    }

    // 상태 확인 헬퍼 메서드
    public boolean isPaymentDone() {
        return "PAYMENT_STATUS_CHANGED".equals(eventType)
                && data != null
                && "DONE".equals(data.getStatus());
    }
}
```

**토스 웹훅 이벤트 타입**:
| 이벤트 | 설명 |
|--------|------|
| PAYMENT_STATUS_CHANGED | 결제 상태 변경 |
| PAYOUT_STATUS_CHANGED | 정산 상태 변경 |
| DEPOSIT_CALLBACK | 가상계좌 입금 |

**결제 상태 (data.status)**:
| 상태 | 설명 |
|------|------|
| READY | 결제 준비 |
| IN_PROGRESS | 결제 진행 중 |
| DONE | 결제 완료 |
| CANCELED | 전체 취소 |
| PARTIAL_CANCELED | 부분 취소 |
| ABORTED | 승인 실패 |
| EXPIRED | 유효기간 만료 |

### 4.2 웹훅 컨트롤러 (TossWebhookApi.java)

```java
@PostMapping("/toss")
public ResponseEntity<String> handleTossWebhook(
        @RequestHeader(value = "Toss-Signature", required = false) String signature,
        @RequestBody TossWebhookPayload payload
) {
    try {
        // 시그니처 검증 (운영 환경 필수)
        // webhookService.verifySignature(signature, payload);

        // 웹훅 처리
        webhookService.processWebhook(payload);

        return ResponseEntity.ok("OK");
    } catch (Exception e) {
        // 에러가 발생해도 200을 반환!
        // (그래야 토스에서 재시도하지 않음)
        log.error("웹훅 처리 실패: {}", e.getMessage());
        return ResponseEntity.ok("FAILED");
    }
}
```

**중요**: 반드시 200 OK를 반환해야 함!
- 200이 아니면 토스에서 웹훅을 재전송함
- 에러 발생 시에도 200 반환 후 별도 모니터링으로 처리

### 4.3 웹훅 처리 서비스 (WebhookService.java)

```java
@Transactional
public void processWebhook(TossWebhookPayload payload) {
    String orderId = payload.getData().getOrderId();

    // 결제 완료
    if (payload.isPaymentDone()) {
        handlePaymentDone(payload);
    }
    // 결제 취소
    else if (payload.isPaymentCanceled()) {
        handlePaymentCanceled(payload);
    }
    // 결제 실패
    else if (payload.isPaymentFailed()) {
        handlePaymentFailed(payload);
    }
}

private void handlePaymentDone(TossWebhookPayload payload) {
    Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);

    // 멱등성: 이미 완료된 결제는 스킵
    if (payment.getStatus() == PaymentStatus.COMPLETED) {
        log.info("이미 완료된 결제 - 웹훅 스킵");
        return;
    }

    // 상태 업데이트
    payment.approve(paymentKey, method);
    paymentRepository.save(payment);

    // 이벤트 발행
    publishPaymentEvent(payment, paymentKey);
}
```

**멱등성 처리**:
- 같은 웹훅이 여러 번 들어와도 안전하게 처리
- 이미 처리된 결제는 스킵
- 상태 전이 규칙 검증 (PENDING → COMPLETED만 허용)

---

## 5. 시그니처 검증

### 왜 필요한가?
> 웹훅 URL이 노출되면 누구나 가짜 요청을 보낼 수 있음

### 검증 방법 (HMAC-SHA256)
```java
public void verifySignature(String signature, TossWebhookPayload payload) {
    String payloadJson = objectMapper.writeValueAsString(payload);

    Mac mac = Mac.getInstance("HmacSHA256");
    SecretKeySpec secretKeySpec = new SecretKeySpec(
            webhookSecretKey.getBytes(), "HmacSHA256");
    mac.init(secretKeySpec);

    byte[] hash = mac.doFinal(payloadJson.getBytes());
    String expectedSignature = Base64.getEncoder().encodeToString(hash);

    if (!expectedSignature.equals(signature)) {
        throw new SecurityException("Invalid webhook signature");
    }
}
```

### 설정
```yaml
# application.yml
PSP:
  toss:
    webhookSecretKey: ${TOSS_WEBHOOK_SECRET_KEY:}
```

**토스 대시보드에서 웹훅 시크릿 키 확인**:
1. [토스 대시보드](https://developers.tosspayments.com) 접속
2. 개발 정보 > 웹훅 설정
3. 시크릿 키 복사 → 환경변수에 설정

---

## 6. 상태 동기화 흐름

### 정상 케이스 (Redirect 성공)
```
1. 사용자 결제 완료
2. 토스 → redirect → /api/payments/confirm (먼저 호출)
3. Payment: PENDING → COMPLETED
4. 토스 → /webhook/toss (나중에 호출)
5. 이미 COMPLETED → 스킵 (멱등성)
```

### 복구 케이스 (Redirect 실패)
```
1. 사용자 결제 완료
2. 토스 → redirect → 네트워크 오류 (실패!)
3. Payment: PENDING 유지
4. 토스 → /webhook/toss (정상 도착)
5. Payment: PENDING → COMPLETED (복구!)
6. 이벤트 발행 (스터디 참여 처리)
```

---

## 7. 에러 처리 전략

| 상황 | 처리 방법 |
|------|----------|
| orderId 없음 | 로깅 후 200 반환 |
| 이미 처리된 상태 | 스킵 후 200 반환 |
| 상태 전이 불가 | 로깅 후 200 반환 |
| 이벤트 발행 실패 | 로깅 후 200 반환 (별도 배치로 재처리) |
| DB 저장 실패 | 로깅 후 200 반환 (토스 재시도로 복구) |

**원칙**: 웹훅 처리 실패해도 200 반환
- 토스 재시도로 무한 루프 방지
- 실패 건은 별도 모니터링/알림으로 처리

---

## 8. 토스 대시보드 설정

### 웹훅 URL 등록
1. [토스 대시보드](https://developers.tosspayments.com) 접속
2. 개발 정보 > 웹훅 설정
3. 웹훅 URL 입력:
   - 테스트: `https://your-test-domain.com/webhook/toss`
   - 운영: `https://your-domain.com/webhook/toss`

### 이벤트 구독 설정
- [x] PAYMENT_STATUS_CHANGED (결제 상태 변경)
- [ ] PAYOUT_STATUS_CHANGED (정산 상태 변경) - 필요 시 선택
- [ ] DEPOSIT_CALLBACK (가상계좌 입금) - 가상계좌 사용 시 선택

### 로컬 테스트 (ngrok 사용)
```bash
# ngrok으로 로컬 서버를 외부에 노출
ngrok http 8083

# 토스 대시보드에 ngrok URL 등록
# https://xxxx-xxx-xxx.ngrok.io/webhook/toss
```

---

## 9. 테스트 방법

### 수동 테스트 (curl)
```bash
curl -X POST http://localhost:8083/webhook/toss \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "PAYMENT_STATUS_CHANGED",
    "createdAt": "2024-01-01T12:00:00+09:00",
    "data": {
      "paymentKey": "test_payment_key",
      "orderId": "S1_M1_abc123",
      "status": "DONE",
      "orderName": "스프링 스터디 참가비",
      "totalAmount": 10000,
      "method": "카드"
    }
  }'
```

### 헬스체크
```bash
curl http://localhost:8083/webhook/toss/health
# 응답: Webhook endpoint is healthy
```

---

## 10. 체크리스트

### 개발 완료 확인
- [x] TossWebhookPayload DTO 생성
- [x] TossWebhookApi 컨트롤러 생성
- [x] WebhookService 처리 로직 구현
- [x] 시그니처 검증 메서드 구현
- [x] application.yml 웹훅 설정 추가

### 운영 배포 전 확인
- [ ] 토스 대시보드에 웹훅 URL 등록
- [ ] TOSS_WEBHOOK_SECRET_KEY 환경변수 설정
- [ ] 웹훅 엔드포인트 방화벽/보안그룹 오픈
- [ ] 시그니처 검증 활성화 (주석 해제)
- [ ] 로깅 레벨 확인 (운영: INFO)

---

## 11. 코드 위치

| 파일 | 경로 | 설명 |
|------|------|------|
| TossWebhookPayload.java | adapter/webapi/dto/ | 웹훅 페이로드 DTO |
| TossWebhookApi.java | adapter/webapi/ | 웹훅 컨트롤러 |
| WebhookService.java | application/ | 웹훅 처리 서비스 |
| TossPaymentProperties.java | adapter/config/ | 웹훅 시크릿 키 설정 |
