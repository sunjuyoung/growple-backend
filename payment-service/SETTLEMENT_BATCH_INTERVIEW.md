# 정산 배치 시스템 (면접용)

## 1. 한 줄 소개

> 스터디 완료 시 참가자들의 출석률에 따라 보증금을 자동으로 환급하는 **Spring Batch 기반 정산 시스템**

---

## 2. 시스템 개요

```
매일 새벽 3시 자동 실행

[Step 1: 정산 생성]
Study Service → 완료된 스터디 조회 → Settlement 생성

[Step 2: 정산 실행]
Settlement 조회 → Member Service로 포인트 지급 → 완료 처리
```

**환급금 계산 공식**:
```
환급금 = 보증금 - (결석 횟수 × 결석당 페널티)
```

---

## 3. 왜 Spring Batch를 선택했는가?

### 일반 스케줄러 vs Spring Batch

| 항목 | 일반 스케줄러 (@Scheduled) | Spring Batch |
|------|--------------------------|--------------|
| 대용량 처리 | 메모리 부족 위험 | Chunk 단위 처리로 안정적 |
| 실패 복구 | 직접 구현 필요 | Skip/Retry 내장 |
| 실행 이력 | 직접 구현 필요 | 메타데이터 테이블 자동 관리 |
| 재시작 | 처음부터 다시 | 실패 지점부터 재시작 |

### 선택 이유

1. **대용량 처리**: 스터디 수백~수천 개를 안정적으로 처리
2. **트랜잭션 안정성**: Chunk 단위 커밋으로 부분 실패 시에도 처리된 데이터 보존
3. **모니터링**: Job/Step 실행 이력 자동 관리
4. **MSA 환경**: 외부 서비스(Study, Member) 호출 실패 시 재시도 처리

---

## 4. 핵심 설계 포인트

### 4.1 2단계 분리 (정산 생성 → 정산 실행)

**왜 분리했는가?**

```
[분리하지 않은 경우]
스터디 조회 → 환급금 계산 → 포인트 지급 (한 트랜잭션)
                              ↓
              포인트 지급 실패 시 전체 롤백 😱
```

```
[분리한 경우]
Step 1: 스터디 조회 → Settlement 저장 (커밋)
Step 2: Settlement 조회 → 포인트 지급 (개별 처리)
                              ↓
              실패 시 해당 건만 재시도 ✅
```

### 4.2 상태 기반 처리 (State Machine)

```
Settlement: PENDING → PROCESSING → COMPLETED
                 ↘              ↗
                   FAILED (재시도 예정)
```

- **PENDING**: 생성됨, 처리 대기
- **PROCESSING**: 처리 중 (동시 처리 방지용 락)
- **COMPLETED**: 완료
- **FAILED**: 실패, 재시도 예정

### 4.3 지수 백오프 재시도

```java
// 재시도 간격: 2분 → 4분 → 8분 → 최종 실패
nextRetryAt = now.plusMinutes((long) Math.pow(2, retryCount));
```

| 재시도 | 대기 시간 | 누적 시간 |
|--------|----------|----------|
| 1회차 | 2분 | 2분 |
| 2회차 | 4분 | 6분 |
| 3회차 | 8분 | 14분 |
| 초과 | 최종 실패 | - |

### 4.4 낙관적 락 (Optimistic Lock)

```java
@Version
private long version;
```

- 동시에 같은 Settlement을 처리하려 할 때 충돌 감지
- 먼저 커밋한 트랜잭션만 성공, 나머지는 재시도

---

## 5. 기술적 챌린지와 해결

### 챌린지 1: 외부 서비스 장애 대응

**문제**: Member Service 장애 시 포인트 지급 실패

**해결**:
```java
try {
    memberRestClient.addPoints(memberId, amount, reason);
    item.markPaid(now, txId);
} catch (Exception e) {
    item.markFailed(now, retryCount + 1, nextRetryAt, e.getMessage());
}
```
- 개별 Item 단위로 실패 처리
- 다음 배치 실행 시 FAILED 상태인 Item만 재시도

### 챌린지 2: 중복 처리 방지

**문제**: 같은 스터디에 대해 Settlement이 중복 생성될 수 있음

**해결**:
```java
// Reader에서 필터링
Set<Long> existingSettlementStudyIds =
    settlementRepository.findStudyIdsWithSettlement(studyIds);

// + DB 유니크 제약조건
@UniqueConstraint(name = "uk_settlement_study", columnNames = {"study_id"})
```

### 챌린지 3: 대용량 처리 시 메모리

**문제**: 스터디 1만 개를 한 번에 조회하면 OOM

**해결**:
```java
.chunk(10, transactionManager)  // 10건씩 처리 후 커밋
```

---

## 6. 개선점 / 추가하고 싶은 것

### 6.1 현재 한계점

| 항목 | 현재 상태 | 개선 방향 |
|------|----------|----------|
| 알림 | 로그만 기록 | Slack/이메일 알림 추가 |
| 모니터링 | 기본 로깅 | Grafana 대시보드 연동 |
| 파티셔닝 | 단일 스레드 | 멀티 스레드 파티셔닝 |
| 정산 취소 | 미구현 | 관리자 정산 취소 기능 |

### 6.2 성능 개선 아이디어

**1. 파티셔닝 (Partitioning)**
```
현재: 순차 처리 (1 스레드)
개선: 스터디 ID 범위별 병렬 처리 (N 스레드)

예: 1~1000 → Worker1, 1001~2000 → Worker2
```

**2. 비동기 포인트 지급**
```
현재: 동기 API 호출 (블로킹)
개선: Kafka 이벤트 발행 → Member Service 비동기 처리
```

**3. 배치 벌크 API**
```
현재: 1건씩 Member Service 호출
개선: 벌크 API로 N건 한 번에 처리
```

### 6.3 운영 관점 개선

**1. 수동 실행 API**
```java
@PostMapping("/admin/settlement/run")
public void runSettlementJob() {
    jobLauncher.run(settlementJob, new JobParametersBuilder()
        .addLocalDateTime("manualRunAt", LocalDateTime.now())
        .toJobParameters());
}
```

**2. 특정 스터디 재정산**
```java
@PostMapping("/admin/settlement/{studyId}/retry")
public void retrySettlement(@PathVariable Long studyId) {
    // 해당 스터디의 FAILED 상태 Settlement 재처리
}
```

---

## 7. 면접 예상 질문

### Q1. 왜 2단계로 나눴나요?

> 정산 생성과 실행을 분리하면 **부분 실패에 대한 복구가 용이**합니다.
> 포인트 지급 실패 시 전체 롤백이 아닌 해당 건만 재시도할 수 있습니다.

### Q2. Chunk 사이즈는 어떻게 정했나요?

> 테스트 결과 10건이 메모리와 성능의 균형점이었습니다.
> 너무 작으면 커밋 오버헤드, 너무 크면 실패 시 재처리 범위가 넓어집니다.

### Q3. 동시성 이슈는 어떻게 처리했나요?

> **낙관적 락(@Version)**과 **상태 기반 선점(claim)**을 사용했습니다.
> PENDING → PROCESSING 전이 시 동시 처리를 방지합니다.

### Q4. 외부 서비스 장애 시 어떻게 되나요?

> **지수 백오프 재시도**로 처리합니다.
> 2분 → 4분 → 8분 간격으로 최대 3회 재시도 후 최종 실패 처리됩니다.
> 최종 실패 건은 관리자 알림 후 수동 처리합니다.

### Q5. 대용량 처리 시 성능은?

> 현재 단일 스레드로 분당 약 600건 처리 가능합니다.
> 성능이 부족하면 **파티셔닝**으로 병렬 처리하거나, **벌크 API**를 도입할 계획입니다.

---

## 8. 요약 (30초 버전)

> 스터디 완료 시 참가자들의 보증금을 자동 환급하는 Spring Batch 정산 시스템입니다.
>
> **2단계 구조**(정산 생성 → 실행)로 부분 실패에 대응하고,
> **지수 백오프 재시도**로 외부 서비스 장애를 처리합니다.
>
> 개선점으로는 파티셔닝을 통한 병렬 처리와 Kafka 기반 비동기 처리를 고려하고 있습니다.
