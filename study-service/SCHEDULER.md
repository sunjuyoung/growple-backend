# Study Scheduler & Event Chaining

## 개요

스터디 생명주기를 자동화하기 위한 스케줄러 시스템입니다.
스터디 상태 변경 시 도메인 이벤트를 발행하고, 이벤트 리스너가 다음 단계의 스케줄러 Job을 생성하는 **이벤트 체이닝** 패턴을 사용합니다.

## 아키텍처

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Event Chaining Flow                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────┐    이벤트 발행    ┌────────────────────┐    Job 생성          │
│  │  Study   │ ───────────────► │SchedulerJobEvent   │ ──────────────────►   │
│  │ (Domain) │                  │    Listener        │                       │
│  └──────────┘                  └────────────────────┘                       │
│       ▲                                                                      │
│       │ 상태 변경                                                            │
│       │                                                                      │
│  ┌────────────────────────┐    Job 처리    ┌───────────────┐                │
│  │ RecruitmentDeadline    │ ◄────────────  │ SchedulerJob  │                │
│  │     Scheduler          │                │   (Entity)    │                │
│  └────────────────────────┘                └───────────────┘                │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 핵심 컴포넌트

### 1. SchedulerJob (도메인 엔티티)

스케줄러 작업을 표현하는 엔티티입니다.

**위치**: `domain/scheduler/SchedulerJob.java`

| 필드 | 설명 |
|------|------|
| `jobType` | 작업 유형 (RECRUITMENT_DEADLINE, STUDY_START, STUDY_COMPLETION) |
| `status` | 작업 상태 (PENDING, PROCESSING, COMPLETED, FAILED) |
| `targetType` | 대상 타입 (예: "STUDY") |
| `targetId` | 대상 ID (예: studyId) |
| `scheduledDate` | 실행 예정일 |
| `retryCount` | 재시도 횟수 |
| `maxRetries` | 최대 재시도 횟수 (기본값: 3) |
| `nextRetryAt` | 다음 재시도 시간 |
| `version` | 낙관적 락 버전 |

**상태 전이**:
```
PENDING ──claim()──► PROCESSING ──complete()──► COMPLETED
    ▲                     │
    │                     │ fail()
    │                     ▼
    └───── 재시도 ─────  FAILED
```

### 2. JobType (작업 유형)

**위치**: `domain/scheduler/JobType.java`

| 유형 | 설명 | 트리거 상태 |
|------|------|-------------|
| `RECRUITMENT_DEADLINE` | 모집 마감 처리 | RECRUITING |
| `STUDY_START` | 스터디 시작 처리 | RECRUIT_CLOSED |
| `STUDY_COMPLETION` | 스터디 종료 처리 | IN_PROGRESS |

### 3. StudyStatusChangedEvent (도메인 이벤트)

**위치**: `domain/event/StudyStatusChangedEvent.java`

스터디 상태가 변경될 때 발행되는 도메인 이벤트입니다.

```java
public record StudyStatusChangedEvent(
    Long studyId,
    StudyStatus newStatus,
    LocalDate recruitEndDate,
    LocalDate startDate,
    LocalDate endDate
) {}
```

**이벤트 발행 시점** (Study 도메인 메서드):
- `openRecruitment()` → RECRUITING 상태로 변경 시
- `closeRecruitment()` → RECRUIT_CLOSED 상태로 변경 시
- `start()` → IN_PROGRESS 상태로 변경 시

### 4. SchedulerJobEventListener (이벤트 리스너)

**위치**: `adapter/scheduler/SchedulerJobEventListener.java`

트랜잭션 커밋 후 이벤트를 수신하여 다음 단계의 Job을 생성합니다.

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onStudyStatusChanged(StudyStatusChangedEvent event) {
    switch (event.newStatus()) {
        case RECRUITING -> // RECRUITMENT_DEADLINE Job 생성
        case RECRUIT_CLOSED -> // STUDY_START Job 생성
        case IN_PROGRESS -> // STUDY_COMPLETION Job 생성
    }
}
```

### 5. RecruitmentDeadlineScheduler (배치 스케줄러)

**위치**: `adapter/scheduler/RecruitmentDeadlineScheduler.java`

매일 00:05 (KST)에 실행되어 모집 마감 Job을 처리합니다.

**처리 로직**:
1. 처리 가능한 Job 조회 (PENDING 또는 FAILED, 재시도 시간 도래)
2. Job 선점 (claim) - 낙관적 락으로 동시성 제어
3. 스터디 조회 및 상태 변경:
   - 최소 인원 충족 → `RECRUIT_CLOSED` (모집 마감)
   - 최소 인원 미달 → `CANCELLED` (취소)
4. Job 완료 처리

## 이벤트 체이닝 플로우

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         스터디 생명주기 자동화                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  [1] 스터디 등록 (PENDING)                                                   │
│         │                                                                    │
│         ▼ openRecruitment()                                                  │
│  [2] 모집 시작 (RECRUITING) ──► StudyStatusChangedEvent 발행                 │
│         │                              │                                     │
│         │                              ▼                                     │
│         │                    ┌─────────────────────┐                        │
│         │                    │ RECRUITMENT_DEADLINE│ Job 생성               │
│         │                    │ scheduledDate =     │                        │
│         │                    │   recruitEndDate    │                        │
│         │                    └─────────────────────┘                        │
│         │                              │                                     │
│         ▼ (recruitEndDate 도래)        ▼                                     │
│  ┌────────────────────────────────────────────┐                             │
│  │ RecruitmentDeadlineScheduler (매일 00:05)  │                             │
│  │ - 최소 인원 충족: closeRecruitment()       │                             │
│  │ - 최소 인원 미달: cancel()                 │                             │
│  └────────────────────────────────────────────┘                             │
│         │                                                                    │
│         ▼ closeRecruitment()                                                 │
│  [3] 모집 마감 (RECRUIT_CLOSED) ──► StudyStatusChangedEvent 발행            │
│         │                              │                                     │
│         │                              ▼                                     │
│         │                    ┌─────────────────────┐                        │
│         │                    │    STUDY_START     │ Job 생성                │
│         │                    │ scheduledDate =    │                         │
│         │                    │     startDate      │                         │
│         │                    └─────────────────────┘                        │
│         │                              │                                     │
│         ▼ (startDate 도래)             ▼                                     │
│  ┌────────────────────────────────────────────┐                             │
│  │ StudyStartScheduler (TODO)                 │                             │
│  │ - study.start() 호출                       │                             │
│  └────────────────────────────────────────────┘                             │
│         │                                                                    │
│         ▼ start()                                                            │
│  [4] 진행 중 (IN_PROGRESS) ──► StudyStatusChangedEvent 발행                 │
│         │                              │                                     │
│         │                              ▼                                     │
│         │                    ┌─────────────────────┐                        │
│         │                    │  STUDY_COMPLETION  │ Job 생성                │
│         │                    │ scheduledDate =    │                         │
│         │                    │      endDate       │                         │
│         │                    └─────────────────────┘                        │
│         │                              │                                     │
│         ▼ (endDate 도래)               ▼                                     │
│  ┌────────────────────────────────────────────┐                             │
│  │ StudyCompletionScheduler (TODO)            │                             │
│  │ - study.complete() 호출                    │                             │
│  │ - 보증금 정산 처리                          │                             │
│  └────────────────────────────────────────────┘                             │
│         │                                                                    │
│         ▼                                                                    │
│  [5] 완료 (COMPLETED)                                                        │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 재시도 전략

Job 처리 실패 시 지수 백오프(Exponential Backoff) 전략을 사용합니다.

```java
// 재시도 간격: 5분 * 3^(retryCount)
// 1차 실패: 5분 후 재시도
// 2차 실패: 15분 후 재시도
// 3차 실패: 45분 후 재시도
// 3회 초과: 재시도 중단 (수동 확인 필요)

private LocalDateTime calculateNextRetry(int currentRetryCount) {
    int delayMinutes = 5 * (int) Math.pow(3, currentRetryCount);
    return LocalDateTime.now().plusMinutes(delayMinutes);
}
```

## 동시성 제어

낙관적 락(Optimistic Locking)을 사용하여 여러 인스턴스에서 동시에 같은 Job을 처리하는 것을 방지합니다.

```java
@Entity
public class SchedulerJob {
    @Version
    private long version;  // 낙관적 락 버전

    public void claim(LocalDateTime now) {
        // 이미 처리 중이거나 완료된 Job은 claim 불가
        if (!status.isClaimable()) {
            throw new IllegalStateException("Job is not claimable");
        }
        // ...
    }
}
```

## 데이터베이스 스키마

```sql
CREATE TABLE scheduler_job (
    id              BIGINT PRIMARY KEY,
    job_type        VARCHAR(30) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    target_type     VARCHAR(20) NOT NULL,
    target_id       BIGINT NOT NULL,
    scheduled_date  DATE NOT NULL,
    processing_started_at TIMESTAMP,
    retry_count     INT NOT NULL DEFAULT 0,
    max_retries     INT NOT NULL DEFAULT 3,
    next_retry_at   TIMESTAMP,
    last_error      VARCHAR(1000),
    completed_at    TIMESTAMP,
    created_at      TIMESTAMP NOT NULL,
    version         BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uk_job_target UNIQUE (job_type, target_type, target_id, scheduled_date)
);

-- 인덱스
CREATE INDEX idx_job_type_status ON scheduler_job (job_type, status, next_retry_at);
CREATE INDEX idx_job_target ON scheduler_job (target_type, target_id);
CREATE INDEX idx_job_scheduled_date ON scheduler_job (scheduled_date);
```

## 패키지 구조

```
com.grow.study
├── domain
│   ├── event
│   │   └── StudyStatusChangedEvent.java    # 도메인 이벤트
│   └── scheduler
│       ├── SchedulerJob.java               # 스케줄러 Job 엔티티
│       ├── JobType.java                    # Job 유형 Enum
│       └── JobStatus.java                  # Job 상태 Enum
├── application
│   ├── SchedulerJobService.java            # Job 생성 서비스
│   └── required
│       └── SchedulerJobRepository.java     # Repository 인터페이스
└── adapter
    ├── scheduler
    │   ├── RecruitmentDeadlineScheduler.java   # 모집 마감 스케줄러
    │   └── SchedulerJobEventListener.java      # 이벤트 리스너
    └── persistence
        ├── SchedulerJobJpaRepository.java      # JPA Repository
        └── SchedulerJobRepositoryAdapter.java  # Repository 구현체
```

## TODO

- [ ] `StudyStartScheduler` 구현 (STUDY_START Job 처리)
- [ ] `StudyCompletionScheduler` 구현 (STUDY_COMPLETION Job 처리)
- [ ] 모집 취소 시 참가자 결제 전액 포인트 환급 처리
- [ ] Job 처리 실패 알림 발송 (Slack, Email 등)
- [ ] 재시도 횟수 초과 Job 모니터링 대시보드
