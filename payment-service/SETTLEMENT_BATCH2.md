# 정산 배치 시스템 (Settlement Batch)

## 개요

Spring Batch 기반의 스터디 정산 시스템입니다.
완료된 스터디의 보증금을 출석률에 따라 참가자에게 환급합니다.

## 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                      settlementJob                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Step 1: createSettlementStep (정산 대상 생성)                    │
│  ┌─────────────────┐  ┌──────────────────────┐  ┌─────────────┐ │
│  │ ExpiredStudy    │→ │ CreateSettlement     │→ │ Settlement  │ │
│  │ Reader          │  │ Processor            │  │ Writer      │ │
│  └─────────────────┘  └──────────────────────┘  └─────────────┘ │
│         ↓                      ↓                      ↓         │
│  Study Service API    Settlement + Item 생성      DB 저장       │
│                                                                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Step 2: executeSettlementStep (정산 실행)                        │
│  ┌─────────────────┐  ┌──────────────────────┐  ┌─────────────┐ │
│  │ Settlement      │→ │ ExecuteSettlement    │→ │ Execute     │ │
│  │ Reader          │  │ Processor            │  │ Writer      │ │
│  └─────────────────┘  └──────────────────────┘  └─────────────┘ │
│         ↓                      ↓                      ↓         │
│  PENDING Settlement    Member Service API        결과 로깅      │
│  조회                  (포인트 지급)                             │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## 실행 스케줄

- **실행 시간**: 매일 새벽 3시 (cron: `0 0 3 * * *`)
- **실행 클래스**: `SettlementBatchScheduler`

## 파일 구조

```
payment-service/src/main/java/com/grow/payment/
├── adapter/
│   ├── batch/                          # Step 2 + Job 설정
│   │   ├── SettlementJobConfig.java        # Job 정의
│   │   ├── SettlementJobListener.java      # Job 리스너
│   │   ├── SettlementBatchScheduler.java   # 스케줄러
│   │   ├── ExecuteSettlementStepConfig.java
│   │   ├── SettlementReader.java
│   │   ├── SettlementExecutionDto.java
│   │   ├── ExecuteSettlementProcessor.java
│   │   ├── ExecuteSettlementWriter.java
│   │   └── ExecuteSettlementStepListener.java
│   │
│   └── config/                         # Step 1 설정
│       ├── BatchConfig.java                # 배치 기본 설정
│       ├── CreateSettlementStepConfig.java
│       ├── ExpiredStudyReader.java
│       ├── ExpiredStudyDto.java
│       ├── CreateSettlementProcessor.java
│       ├── SettlementCreationResult.java
│       ├── SettlementWriter.java
│       └── CreateSettlementStepListener.java
│
├── domain/settlement/
│   ├── Settlement.java                 # 정산 헤더 엔티티
│   ├── SettlementItem.java             # 정산 아이템 엔티티
│   ├── SettlementStatus.java           # PENDING, PROCESSING, COMPLETED, FAILED
│   ├── SettlementItemStatus.java       # PENDING, PAYOUT_DONE, FAILED
│   └── PayoutMethod.java               # POINT
│
└── application/
    ├── required/
    │   ├── SettlementRepository.java
    │   ├── SettlementItemRepository.java
    │   ├── StudyRestClient.java        # Study Service 통신
    │   └── MemberRestClient.java       # Member Service 통신
    └── dto/
        └── CompletedStudyForSettlementResponse.java
```

## Step 상세

### Step 1: createSettlementStep

**목적**: Study Service에서 완료된 스터디를 조회하여 Settlement 생성

| 컴포넌트 | 설명 |
|---------|------|
| `ExpiredStudyReader` | Study Service API 호출하여 COMPLETED 스터디 목록 조회 |
| `CreateSettlementProcessor` | Settlement 헤더 + SettlementItem 생성, 환급금 계산 |
| `SettlementWriter` | Settlement과 Items를 DB에 저장 |

**환급금 계산 로직**:
```
penaltyAmount = min(absenceCount × penaltyPerAbsence, originalAmount)
refundAmount = originalAmount - penaltyAmount
```

### Step 2: executeSettlementStep

**목적**: PENDING 상태의 Settlement에 대해 포인트 지급 실행

| 컴포넌트 | 설명 |
|---------|------|
| `SettlementReader` | PENDING/FAILED 상태의 Settlement 조회 |
| `ExecuteSettlementProcessor` | Member Service API 호출하여 포인트 지급 |
| `ExecuteSettlementWriter` | 결과 로깅 |

## 상태 전이

### Settlement
```
PENDING → PROCESSING → COMPLETED
              ↓
           FAILED (재시도 가능)
```

### SettlementItem
```
PENDING → PAYOUT_DONE
    ↓
  FAILED (재시도 가능)
```

## 재시도 메커니즘

- **최대 재시도 횟수**: 3회
- **백오프 전략**: 지수 백오프 (`2^retryCount` 분)
- **재시도 스케줄링**: `nextRetryAt` 필드로 다음 실행 시간 지정

```
1차 실패: 2분 후 재시도
2차 실패: 4분 후 재시도
3차 실패: 8분 후 재시도
4차 실패: 최대 횟수 초과, 재시도 중단
```

## 외부 서비스 통신

### Study Service
```
GET  /internal/studies/completed-for-settlement?limit={limit}
POST /internal/studies/{studyId}/mark-settled
```

### Member Service
```
POST /internal/members/{memberId}/refund
GET  /internal/members/{memberId}/point
```

## 설정

### application.yml
```yaml
spring:
  batch:
    job:
      enabled: false  # 애플리케이션 시작 시 자동 실행 방지
    jdbc:
      initialize-schema: always  # 배치 메타 테이블 자동 생성
```

### Chunk Size
- Step 1: 10건
- Step 2: 5건

### Skip Limit
- Step 1: 5건
- Step 2: 3건

## 수동 실행

배치를 수동으로 실행하려면 `SettlementBatchScheduler.runSettlementJob()` 호출:

```java
@Autowired
private SettlementBatchScheduler scheduler;

scheduler.runSettlementJob();
```

## 모니터링

### 로그 레벨 설정
```yaml
logging:
  level:
    com.grow.payment: DEBUG
    org.springframework.batch: INFO
```

### 주요 로그 포인트
- Job 시작/종료
- Step 시작/종료
- 처리 건수 (Read/Write/Skip/Filter)
- 포인트 지급 성공/실패

## 향후 확장 계획

### 2단계: 이벤트 기반 추가
```
StudyCompletedEvent 수신 → Settlement 즉시 생성
기존 배치는 Sweep + 정산 실행 역할로 유지
```

## 테이블 스키마

### settlement
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT | PK |
| study_id | BIGINT | 스터디 ID (UK) |
| status | VARCHAR(20) | 상태 |
| processing_started_at | TIMESTAMP | 배치 선점 시간 |
| retry_count | INT | 재시도 횟수 |
| next_retry_at | TIMESTAMP | 다음 재시도 시간 |
| last_error | VARCHAR(1000) | 마지막 에러 |
| settled_at | TIMESTAMP | 정산 완료 시간 |
| version | BIGINT | 낙관적 락 |
| created_at | TIMESTAMP | 생성 시간 |

### settlement_item
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT | PK |
| settlement_id | BIGINT | 정산 ID |
| participant_id | BIGINT | StudyMember ID |
| member_id | BIGINT | 회원 ID |
| original_amount | BIGINT | 원금(보증금) |
| absence_count | INT | 결석 횟수 |
| penalty_amount | BIGINT | 페널티 금액 |
| refund_amount | BIGINT | 환급 금액 |
| payout_method | VARCHAR(20) | 지급 방식 |
| status | VARCHAR(20) | 상태 |
| retry_count | INT | 재시도 횟수 |
| next_retry_at | TIMESTAMP | 다음 재시도 시간 |
| processed_point_tx_id | BIGINT | 포인트 트랜잭션 ID |
| processed_at | TIMESTAMP | 처리 시간 |
| version | BIGINT | 낙관적 락 |
