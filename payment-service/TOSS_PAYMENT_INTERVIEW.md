# 토스페이먼츠 결제 시스템 (면접용)

## 1. 한 줄 소개

> 스터디 참가비 결제를 위한 **토스페이먼츠 API 기반 결제 시스템** (테스트 모드)

---

## 2. 시스템 개요

```
[결제 플로우 - Redirect 방식 + 웹훅]

1. 결제 요청 (POST /api/payments)
   └→ Payment 생성 (PENDING) → orderId 발급

2. 토스 결제창 (프론트엔드)
   └→ 사용자가 카드/간편결제 선택 후 결제

3. 결제 승인 (POST /api/payments/confirm)
   └→ 토스 승인 API 호출 → 성공 시 COMPLETED
   └→ Kafka 이벤트 발행 (스터디 상태에 따라)

4. 웹훅 수신 (POST /webhook/toss) [백업 채널]
   └→ redirect 실패 시에도 상태 동기화 보장
   └→ 멱등성 처리로 중복 처리 방지
```

**두 가지 결제 시나리오**:
```
[스터디 생성 결제]
PENDING 상태 스터디 → 결제 완료 → RECRUITING 상태로 전환

[스터디 참여 결제]
RECRUITING 상태 스터디 → 결제 완료 → 참가자로 등록
```

---

## 3. 왜 이렇게 설계했는가?

### 3.1 Redirect 방식 선택

| 방식 | 장점 | 단점 |
|------|------|------|
| **Redirect (현재)** | 구현 단순, PCI DSS 불필요 | 페이지 이동 필요 |
| JavaScript SDK | UX 우수 | 복잡한 프론트엔드 |
| 브랜드페이 | 최고 UX | 심사 필요 |

**선택 이유**:
- MVP 단계에서 빠른 구현이 필요
- 카드 정보를 직접 다루지 않아 보안 부담 없음
- 토스가 결제 UI를 제공하므로 프론트엔드 작업 최소화

### 3.2 2단계 결제 프로세스 (Request → Confirm)

```
[왜 분리?]

1. 결제 요청 시점: orderId 생성, 금액/주문 정보 저장
2. 결제 승인 시점: 토스에서 paymentKey 받아 최종 승인

→ 중간에 사용자가 이탈해도 PENDING 상태로 남음
→ 금액 위변조 검증 가능 (request 시 저장한 금액 vs confirm 시 금액)
```

### 3.3 Kafka 이벤트 기반 통신

**문제**: 결제 완료 후 Study Service에 어떻게 알릴 것인가?

| 방식 | 장점 | 단점 |
|------|------|------|
| **Kafka 이벤트 (현재)** | 느슨한 결합, 비동기 처리 | 최종 일관성 |
| 동기 API 호출 | 즉시 일관성 | 강한 결합, 장애 전파 |

**선택 이유**:
```java
// 스터디 상태에 따라 다른 이벤트 발행
if(studySummary.status().equals("PENDING")) {
    paymentPublisher.publishStudyCreatedEvent(...);  // 스터디 생성 완료
} else if (studySummary.status().equals("RECRUITING")) {
    paymentPublisher.publishPaymentEnrolledEvent(...);  // 참가자 등록
}
```
- Study Service 장애 시에도 결제 완료 처리 가능
- 이벤트 재처리 가능 (Kafka 특성)

---

## 4. 핵심 설계 포인트

### 4.1 멱등성 (Idempotency) 처리

**문제**: 같은 결제가 중복으로 처리되면?

**해결책 1 - orderId 생성 규칙**:
```java
// studyId + memberId 조합으로 고유성 보장
private String generateOrderId(Long studyId, Long memberId) {
    String uuid = UUID.randomUUID().toString().replace("-", "");
    return String.format("S%d_M%d_%s", studyId, memberId, uuid.substring(0, 12));
}
```

**해결책 2 - 중복 결제 방지**:
```java
Optional<Payment> existing = paymentRepository.findByOrderId(orderId);

if (existing.isPresent()) {
    Payment payment = existing.get();

    // 이미 완료된 결제
    if (payment.getStatus() == PaymentStatus.COMPLETED) {
        throw new AlreadyPaidException("이미 결제가 완료된 스터디입니다.");
    }

    // PENDING이면 재사용 (멱등성)
    if (payment.getStatus() == PaymentStatus.PENDING) {
        return PaymentResponse.from(payment);
    }
}
```

**해결책 3 - 토스 Idempotency-Key**:
```java
tossRestClient.post()
    .header("Idempotency-Key", orderId)  // 토스 측 중복 요청 방지
    .body(request)
    ...
```

### 4.2 상태 기반 처리 (State Machine)

```
Payment: PENDING → COMPLETED
              ↘        ↓
               FAILED  CANCELLED
```

| 상태 | 설명 | 전이 조건 |
|------|------|----------|
| PENDING | 결제 대기 | 주문 생성 시 |
| COMPLETED | 결제 완료 | 토스 승인 성공 |
| FAILED | 결제 실패 | 토스 승인 실패 |
| CANCELLED | 결제 취소 | 사용자/관리자 취소 |

```java
// 상태 전이는 도메인 메서드에서만
public void approve(String paymentKey, String method) {
    this.paymentKey = paymentKey;
    this.method = method;
    this.status = PaymentStatus.COMPLETED;
    this.approvedAt = LocalDateTime.now();
}
```

### 4.3 금액 검증

```java
// 결제 승인 시 금액 위변조 검증
if (!payment.getAmount().equals(command.amount())) {
    payment.fail("금액이 일치하지 않습니다.");
    throw new IllegalArgumentException("결제 금액이 일치하지 않습니다.");
}
```

- Request 시 저장한 금액과 Confirm 시 금액 비교
- 프론트엔드에서 금액 조작 시도 차단

---

## 5. 기술적 챌린지와 해결

### 챌린지 1: 토스 API 인증

**토스 API 인증 방식**: Basic Auth (Secret Key)

```java
// Secret Key를 Base64 인코딩
Base64.Encoder encoder = Base64.getEncoder();
byte[] encodedBytes = encoder.encode((secretKey + ":").getBytes("UTF-8"));
String encodedSecretKey = "Basic " + new String(encodedBytes);

tossRestClient.post()
    .header(HttpHeaders.AUTHORIZATION, encodedSecretKey)
    ...
```

### 챌린지 2: 에러 처리

```java
// 토스 API 에러 처리
.onStatus(HttpStatusCode::isError, (req, res) -> {
    log.error("토스 결제 승인 실패: status={}", res.getStatusCode());
    throw new TossPaymentException("결제 승인 실패: " + res.getStatusCode());
})
```

```java
// 서비스 레벨 에러 처리
try {
    TossConfirmResponse tossResponse = tossPaymentClient.confirm(...);
    payment.approve(command.paymentKey(), tossResponse.getPaymentMethod());
} catch (Exception e) {
    payment.fail(e.getMessage());  // 실패 사유 저장
    throw new IllegalStateException("결제 승인에 실패했습니다.");
}
```

### 챌린지 3: MSA 환경에서 스터디 상태 확인

```java
// Study Service API 호출
StudySummaryResponse studySummary = studyRestClient.getMemberSummary(studyId);

// 상태에 따른 분기 처리
if(studySummary.status().equals("PENDING")) {
    // 스터디 생성 결제
} else if (studySummary.status().equals("RECRUITING")) {
    // 스터디 참여 결제
} else {
    throw new IllegalStateException("참여할 수 없는 상태");
}
```

---

## 6. 구현 완료 / 개선점

### 6.1 구현 현황

| 항목 | 상태 | 설명 |
|------|------|------|
| 웹훅 | **구현 완료** | 토스 웹훅으로 결제 상태 동기화 |
| 재시도 | 미구현 | 토스 API 실패 시 재시도 로직 |
| 취소 | DB만 변경 | 실제 토스 취소 API 호출 필요 |
| 정산 연동 | 배치 구현 | Spring Batch로 정산 처리 |

### 6.2 웹훅 구현 (완료)

**해결한 문제**:
```
[Before - redirect 실패 시]
토스: DONE / 우리 시스템: PENDING (불일치!)

[After - 웹훅 도입]
토스 → 웹훅으로 DONE 알림 → PENDING → COMPLETED (동기화!)
```

**구현 내용**:
```java
@PostMapping("/webhook/toss")
public ResponseEntity<String> handleTossWebhook(
        @RequestHeader(value = "Toss-Signature", required = false) String signature,
        @RequestBody TossWebhookPayload payload
) {
    // 시그니처 검증 (운영 환경 필수)
    // webhookService.verifySignature(signature, payload);

    // 웹훅 처리
    webhookService.processWebhook(payload);

    return ResponseEntity.ok("OK");  // 반드시 200 반환!
}
```

**핵심 처리 로직**:
```java
// 멱등성 처리 - 이미 완료된 결제는 스킵
if (payment.getStatus() == PaymentStatus.COMPLETED) {
    log.info("이미 완료된 결제 - 웹훅 스킵");
    return;
}

// 상태 업데이트 + 이벤트 발행
payment.approve(paymentKey, method);
publishPaymentEvent(payment, paymentKey);
```

### 6.3 결제 재시도 로직 (개선 예정)

```java
// 현재: 단순 실패
try {
    tossPaymentClient.confirm(...);
} catch (Exception e) {
    payment.fail(e.getMessage());
    throw ...;
}

// 개선: 지수 백오프 재시도
@Retryable(
    value = TossPaymentException.class,
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
public TossConfirmResponse confirmWithRetry(...) {
    return tossPaymentClient.confirm(...);
}
```

### 6.4 결제 대사 (Reconciliation)

```
현재: 결제 데이터가 토스와 불일치할 수 있음
개선: 일일 배치로 토스 결제 내역과 DB 비교 → 불일치 건 알림
```

### 6.5 부분 취소 지원

```java
// 현재: 전체 취소만 가능
payment.cancel();

// 개선: 부분 취소 지원
payment.partialCancel(cancelAmount);
tossPaymentClient.cancel(paymentKey, cancelAmount, cancelReason);
```

---

## 7. 면접 예상 질문

### Q1. 왜 Redirect 방식을 선택했나요?

> MVP 단계에서 **빠른 구현**이 우선이었습니다.
> 카드 정보를 직접 다루지 않아 **PCI DSS 인증 불필요**하고,
> 토스가 결제 UI를 제공하므로 프론트엔드 작업을 최소화할 수 있었습니다.

### Q2. 중복 결제는 어떻게 방지하나요?

> **3중 방어**를 적용했습니다:
> 1. orderId 생성 시 studyId + memberId 조합으로 고유성 부여
> 2. 결제 요청 시 기존 PENDING/COMPLETED 상태 체크
> 3. 토스 API 호출 시 Idempotency-Key 헤더 사용

### Q3. 결제 완료 후 Study Service에 어떻게 알리나요?

> **Kafka 이벤트**를 발행합니다.
> - 스터디 생성 결제: `study.created` 토픽
> - 스터디 참여 결제: `payment.enrolled` 토픽
>
> 동기 API 대신 이벤트를 사용해 **느슨한 결합**을 유지하고,
> Study Service 장애 시에도 결제 처리가 가능합니다.

### Q4. 금액 위변조는 어떻게 막나요?

> 결제 승인 시 **금액 검증**을 수행합니다.
> Request 단계에서 저장한 금액과 Confirm 단계의 금액을 비교해
> 불일치 시 결제를 실패 처리합니다.

### Q5. 웹훅을 어떻게 구현했나요?

> **문제**: Redirect 실패 시 토스와 우리 시스템 간 상태 불일치 발생
>
> **해결**: 토스 웹훅을 통해 결제 상태를 동기화합니다.
> - `POST /webhook/toss` 엔드포인트로 이벤트 수신
> - DONE/CANCELED/ABORTED 상태에 따라 처리
> - **멱등성**: 이미 처리된 결제는 스킵
> - **시그니처 검증**: HMAC-SHA256으로 위조 방지
>
> 자세한 내용은 `WEBHOOK_IMPLEMENTATION.md` 참고

### Q6. 현재 시스템의 한계점은?

> 1. **재시도 로직 없음**: 토스 API 일시 장애 시 복구 어려움
> 2. **취소 API 미연동**: 현재 DB 상태만 변경, 토스 취소 API 미호출
> 3. **결제 대사 미구현**: 토스와 DB 간 불일치 건 자동 탐지 필요
>
> 향후 지수 백오프 재시도와 결제 대사 배치를 추가할 계획입니다.

### Q7. 테스트 환경과 운영 환경의 차이는?

> 토스페이먼츠는 **테스트 키**와 **운영 키**를 분리 제공합니다.
> - 테스트: 가상 결제, 실제 카드 청구 없음
> - 운영: 실제 결제, 정산 처리
>
> 환경변수로 키를 관리하며, 코드 변경 없이 전환 가능합니다.

---

## 8. 요약 (30초 버전)

> 토스페이먼츠 Redirect 방식으로 스터디 참가비 결제를 처리합니다.
>
> **2단계 프로세스**(Request → Confirm)로 금액 위변조를 방지하고,
> **Kafka 이벤트**로 Study Service와 느슨하게 연동합니다.
>
> **웹훅**을 통해 redirect 실패 시에도 결제 상태 동기화를 보장하며,
> 멱등성 처리로 중복 요청을 안전하게 처리합니다.
>
> 개선점으로는 결제 대사 배치와 지수 백오프 재시도를 고려하고 있습니다.

---

## 9. 코드 구조

```
payment-service/
├── adapter/
│   ├── webapi/
│   │   ├── PaymentApi.java              # 결제 REST 컨트롤러
│   │   ├── TossWebhookApi.java          # 웹훅 컨트롤러 (NEW)
│   │   └── dto/
│   │       └── TossWebhookPayload.java  # 웹훅 페이로드 DTO (NEW)
│   ├── integration/
│   │   ├── TossPaymentClient.java       # 토스 API 클라이언트
│   │   └── PaymentProducer.java         # Kafka 이벤트 발행
│   └── config/
│       └── TossPaymentProperties.java   # 토스 설정 (webhookSecretKey 추가)
├── application/
│   ├── PaymentService.java              # 결제 비즈니스 로직
│   └── WebhookService.java              # 웹훅 처리 서비스 (NEW)
└── domain/
    ├── Payment.java                     # 결제 엔티티
    └── enums/
        └── PaymentStatus.java           # 결제 상태
```

**관련 문서**:
- `WEBHOOK_IMPLEMENTATION.md` - 웹훅 구현 상세 가이드
