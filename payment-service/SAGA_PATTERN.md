# 스터디 결제 SAGA 패턴

## 1. 한 줄 소개

> 스터디 참여 결제 시 **Kafka 기반 Choreography SAGA 패턴**으로 결제-등록 간 분산 트랜잭션과 보상 트랜잭션(환불)을 처리하는 시스템

---

##   SAGA 패턴

### 분산 트랜잭션의 문제

| 방식 | 장점 | 단점 |
|------|------|------|
| 2PC (Two-Phase Commit) | 강한 일관성 | 동기 방식, 단일 실패 시 전체 블로킹 |
| **SAGA 패턴** | 비동기, 서비스 독립성 | 최종 일관성, 보상 로직 필요 |

### 선택 이유

1. **MSA 환경**: Payment, Study 서비스가 독립 배포/확장
2. **가용성**: 한 서비스 장애가 다른 서비스에 영향 최소화
3. **느슨한 결합**: Kafka를 통한 이벤트 기반 통신
4. **확장성**: 새로운 서비스 추가 시 토픽 구독만 추가

### Choreography vs Orchestration

| 방식 | 설명 | 적합한 경우 |
|------|------|------------|
| **Choreography** (선택) | 각 서비스가 이벤트 발행/구독 | 단순한 플로우, 서비스 2~3개 |
| Orchestration | 중앙 조정자가 흐름 제어 | 복잡한 플로우, 서비스 다수 |

현재 결제 → 등록의 **2단계 플로우**이므로 Choreography 방식 선택

---

## 4. 핵심 설계 포인트

### 4.1 이벤트 토픽 구조

```
Topics:
├── payment.enrolled          # 결제 완료 → 멤버 등록 요청
├── study.enrollment.failed   # 등록 실패 → 환불 요청 (보상)
└── study.created             # 스터디 생성 결제 완료
```

### 4.2 이벤트 메시지 설계 (멱등성 키 포함)

```java
// 결제 완료 이벤트
public record PaymentEnrollmentEvent(
    String eventId,      // UUID - 멱등성 키
    Long userId,
    Long studyId,
    String orderName,
    Integer amount,
    String paymentKey    // 환불 시 필요
) {}

// 환불 요청 이벤트 (보상 트랜잭션)
public record RefundRequestEvent(
    String eventId,      // UUID - 멱등성 키
    Long userId,
    Long studyId,
    String orderName,
    Integer amount,
    String paymentKey,
    String reason        // 실패 사유
) {}
```

### 4.3 재시도 전략 (@RetryableTopic)

```java
@RetryableTopic(
    attempts = "3",
    backoff = @Backoff(delay = 1000, multiplier = 2),
    dltTopicSuffix = ".dlt"
)
```

| 재시도 | 대기 시간 | 토픽 |
|--------|----------|------|
| 1회차 | 1초 | payment.enrolled-retry-0 |
| 2회차 | 2초 | payment.enrolled-retry-1 |
| 3회차 | 4초 | payment.enrolled-retry-2 |
| 최종 실패 | - | payment.enrolled.dlt |

### 4.4 예외 분류 (재시도 vs 즉시 보상)

```java
// Study Service - 이벤트 핸들러
public void handlePaymentEnroll(PaymentEnrollmentEvent event) {
    try {
        studyRegister.enrollment(event.studyId(), event.userId(), event.amount());
    } catch (NonRetryableException e) {
        // 비즈니스 오류 → 즉시 환불 (재시도 의미 없음)
        // 예: 정원 초과, 이미 등록됨, 스터디 종료됨
        studyProducer.publishStudyEnrolledFailedEvent(event, e.getMessage());
    } catch (Exception e) {
        // 기술 오류 → Slack 알림 + @RetryableTopic 재시도
        slackNotifier.sendError("스터디 참여 등록 중 오류 발생", e.getMessage());
    }
}
```

---

## 5. 전체 플로우 상세

```
┌─────────┐     ┌──────────────────┐     ┌─────────────────┐
│  사용자  │     │  Payment Service │     │  Study Service  │
└────┬────┘     └────────┬─────────┘     └────────┬────────┘
     │                   │                        │
     │ 1. 결제 요청       │                        │
     │──────────────────►│                        │
     │                   │                        │
     │                   │ 2. 토스 결제 승인       │
     │                   │ (confirmPayment)       │
     │                   │                        │
     │                   │ 3. Kafka 발행          │
     │                   │ [payment.enrolled]     │
     │                   │───────────────────────►│
     │                   │                        │
     │                   │                        │ 4. 멤버 등록 시도
     │                   │                        │
     │                   │    [성공 시]            │
     │                   │                        │ 등록 완료 ✅
     │                   │                        │
     │                   │    [실패 시]            │
     │                   │ 5. Kafka 발행          │
     │                   │◄───────────────────────│
     │                   │ [study.enrollment.failed]
     │                   │                        │
     │                   │ 6. 토스 결제 취소       │
     │                   │ (cancelByPaymentKey)   │
     │                   │                        │
     │ 7. 환불 완료       │                        │
     │◄──────────────────│                        │
     │                   │                        │
```

---

## 6. 기술적 챌린지와 해결

### 챌린지 1: 메시지 유실

**문제**: Kafka 메시지가 유실되면 결제는 됐는데 등록 안 됨

**해결**:
- Kafka `acks=all` 설정으로 브로커 복제 보장
- `@RetryableTopic`으로 처리 실패 시 재시도
- DLT(Dead Letter Topic)로 최종 실패 메시지 보관

### 챌린지 2: 중복 처리 (멱등성) - 3계층 방어

**문제**: 같은 이벤트가 2번 처리되면 중복 등록/환불

**해결**: 3계층 멱등성 보장

```
┌─────────────────────────────────────────────────────────┐
│                  멱등성 보장 레이어                       │
├─────────────────────────────────────────────────────────┤
│ Layer 1. 이벤트 ID    │ processed_events 테이블 체크     │
│ Layer 2. 비즈니스     │ Study.isMember() + UK 제약조건   │
│ Layer 3. 동시성       │ @Version 낙관적 락               │
└─────────────────────────────────────────────────────────┘
```

**Layer 1: 이벤트 ID 기반 중복 체크**
```java
// processed_events 테이블
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {
    @Id
    private String eventId;      // UUID
    private String eventType;
    private LocalDateTime processedAt;
}

// 핸들러에서 체크
@Transactional
public void handlePaymentEnroll(PaymentEnrollmentEvent event) {
    // 이미 처리된 이벤트면 스킵 (멱등성 보장)
    if (processedEventRepository.existsById(event.eventId())) {
        log.info("이미 처리된 이벤트: eventId={}", event.eventId());
        return;
    }

    studyRegister.enrollment(event.studyId(), event.userId(), event.amount());

    // 처리 완료 기록
    processedEventRepository.save(ProcessedEvent.of(event.eventId(), "PAYMENT_ENROLLED"));
}
```

**Layer 2: 비즈니스 로직 + DB 제약조건**
```java
// Study.java - 도메인 레벨 체크
if (isMember(memberId)) {
    throw new NonRetryableException("이미 참가한 멤버입니다.");
}

// StudyMember 테이블 - DB 레벨 체크
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"study_id", "member_id"})
})
```

**Layer 3: 낙관적 락**
```java
// Payment.java - 동시 취소 요청 방지
@Version
private Long version;
```

### 챌린지 3: 순서 보장

**문제**: 결제 완료 전에 환불 이벤트가 먼저 처리될 수 있음

**해결**:
- 같은 `studyId`를 Kafka 파티션 키로 사용 → 순서 보장
- Payment 상태 검증 후 취소 처리

```java
public PaymentResponse cancelByPaymentKey(String paymentKey, String reason) {
    Payment payment = paymentRepository.findByPaymentKey(paymentKey)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결제"));

    if (!payment.isCancellable()) {
        throw new IllegalStateException("취소할 수 없는 상태: " + payment.getStatus());
    }
    // ...
}
```

---

## 7. 개선점 / 추가하고 싶은 것

### 7.1 구현 완료 항목

| 항목 | 구현 내용 |
|------|----------|
| 멱등성 보장 | `processed_events` 테이블 + eventId 기반 중복 체크 |
| 비즈니스 중복 방어 | `NonRetryableException` + `@UniqueConstraint` |
| 동시성 제어 | `@Version` 낙관적 락 |

### 7.2 추가 개선 방향

| 항목 | 현재 상태 | 개선 방향 |
|------|----------|----------|
| 성공 이벤트 | 미발행 | `study.enrolled.success` 발행 |
| Saga 상태 추적 | 없음 | saga_state 테이블로 추적 |
| 타임아웃 | 없음 | 일정 시간 후 자동 보상 |
| 모니터링 | Slack 알림 | Kafka Lag 모니터링 대시보드 |
| 이벤트 만료 | 없음 | processed_events 주기적 정리 |

### 7.3 Orchestration 전환 고려

서비스가 늘어나면 (알림, 포인트 적립 등):

```
현재 (Choreography):
Payment → Study → (직접 연결)

개선 (Orchestration):
Payment → Saga Orchestrator → Study
                           → Notification
                           → Point
```

### 7.4 이벤트 소싱 도입

```
현재: 상태 기반 (Payment.status = CANCELLED)
개선: 이벤트 기반 (PaymentCreated → PaymentConfirmed → PaymentCancelled)
```

---
