# Spring Batch 정산 시스템 가이드

## 목차
1. [Spring Batch란?](#1-spring-batch란)
2. [핵심 개념](#2-핵심-개념)
3. [정산 배치 전체 흐름](#3-정산-배치-전체-흐름)
4. [코드 상세 분석](#4-코드-상세-분석)
5. [데이터 흐름 다이어그램](#5-데이터-흐름-다이어그램)
6. [주요 설정 옵션](#6-주요-설정-옵션)

---

## 1. Spring Batch란?

Spring Batch는 **대용량 데이터를 일괄 처리**하기 위한 프레임워크입니다.

### 왜 필요한가?

일반적인 REST API로 처리하기 어려운 작업들이 있습니다:
- 수천~수만 건의 데이터를 한 번에 처리
- 매일 새벽에 자동으로 실행되어야 하는 작업
- 실패 시 재시도가 필요한 작업
- 처리 상태를 추적해야 하는 작업

**이 프로젝트의 예시**: 스터디가 완료되면 참가자들에게 보증금을 환급해야 합니다.
매일 새벽 3시에 완료된 스터디를 찾아서, 각 참가자의 출석률을 계산하고, 포인트를 지급합니다.

---

## 2. 핵심 개념

Spring Batch는 레고 블록처럼 조립해서 사용합니다.

### 2.1 Job (작업)

**가장 큰 단위**입니다. 하나의 배치 작업 전체를 의미합니다.

```
┌─────────────────────────────────────────────┐
│                    Job                       │
│  "정산 배치" = 전체 정산 프로세스              │
└─────────────────────────────────────────────┘
```

**코드 위치**: `SettlementJobConfig.java:40-50`

```java
@Bean
public Job settlementJob(Step createSettlementStep, Step executeSettlementStep) {
    return new JobBuilder("settlementJob", jobRepository)
            .incrementer(new RunIdIncrementer())  // 같은 파라미터로 재실행 허용
            .start(createSettlementStep)          // Step 1 실행
            .next(executeSettlementStep)          // Step 2 실행
            .listener(new SettlementJobListener()) // 시작/종료 시 로깅
            .build();
}
```

### 2.2 Step (단계)

Job 안에서 **순차적으로 실행되는 작업 단위**입니다.

```
┌─────────────────────────────────────────────┐
│                    Job                       │
├─────────────────────────────────────────────┤
│  Step 1: 정산 대상 생성                       │
│         (스터디 조회 → Settlement 생성)        │
├─────────────────────────────────────────────┤
│  Step 2: 정산 실행                           │
│         (포인트 지급 → 상태 완료)              │
└─────────────────────────────────────────────┘
```

### 2.3 Chunk (묶음 처리)

**가장 중요한 개념!** 데이터를 한 건씩 처리하지 않고, **묶음(Chunk) 단위로 처리**합니다.

```
예: chunk(10) 설정 시

[데이터 100건이 있다면]

1회차: 10건 읽기 → 10건 처리 → 10건 저장 → 커밋!
2회차: 10건 읽기 → 10건 처리 → 10건 저장 → 커밋!
...
10회차: 10건 읽기 → 10건 처리 → 10건 저장 → 커밋!
```

**장점**:
- 메모리 효율적 (100건을 한 번에 메모리에 올리지 않음)
- 실패 시 해당 Chunk만 재시도 (이미 커밋된 건은 안전)

### 2.4 Reader → Processor → Writer 패턴

각 Step은 세 가지 역할로 구성됩니다:

```
┌──────────┐    ┌──────────┐    ┌──────────┐
│  Reader  │ → │ Processor │ → │  Writer  │
│  (읽기)  │    │  (처리)  │    │  (쓰기)  │
└──────────┘    └──────────┘    └──────────┘
   DB/API          변환/계산        DB 저장
```

| 역할 | 설명 | 이 프로젝트에서는? |
|------|------|------------------|
| **Reader** | 데이터를 읽어옴 | Study Service API 호출 |
| **Processor** | 데이터를 가공/변환 | 환급금 계산 |
| **Writer** | 결과를 저장 | DB에 Settlement 저장 |

---

## 3. 정산 배치 전체 흐름

### 3.1 언제 실행되나요?

**매일 새벽 3시**에 자동 실행됩니다.

**코드 위치**: `SettlementBatchScheduler.java:33`

```java
@Scheduled(cron = "0 0 3 * * *")  // 초 분 시 일 월 요일
public void runSettlementJob() {
    // 배치 실행
}
```

### 3.2 비즈니스 흐름

```
[Step 1: 정산 대상 생성]

   Study Service                    Payment Service
   ┌─────────────┐                 ┌─────────────────┐
   │ COMPLETED   │  ──API 호출──→  │ Settlement 생성 │
   │ 스터디 목록  │                 │ (PENDING 상태)  │
   └─────────────┘                 └─────────────────┘


[Step 2: 정산 실행]

   Payment Service                  Member Service
   ┌─────────────────┐             ┌─────────────────┐
   │ PENDING 상태의   │ ──API 호출──→│ 포인트 지급     │
   │ Settlement 조회  │             │                 │
   └─────────────────┘             └─────────────────┘
           │
           ▼
   상태 → COMPLETED
```

---

## 4. 코드 상세 분석

### 4.1 Step 1: 정산 대상 생성

#### Reader: `ExpiredStudyReader.java`

**역할**: Study Service에서 완료된 스터디 목록을 조회합니다.

```java
@Override
public ExpiredStudyDto read() {
    if (!initialized) {
        initialize();  // 최초 1회만 API 호출
    }

    if (studyIterator.hasNext()) {
        return studyIterator.next();  // 하나씩 반환
    }
    return null;  // null 반환 = 더 이상 데이터 없음
}
```

**핵심 포인트**:
- `ItemReader.read()` 메서드는 **한 건씩** 반환합니다
- `null`을 반환하면 "더 이상 데이터 없음"을 의미합니다
- 이미 정산이 생성된 스터디는 **필터링**합니다

#### Processor: `CreateSettlementProcessor.java`

**역할**: 스터디 정보를 받아서 Settlement과 SettlementItem을 생성합니다.

```java
@Override
public SettlementCreationResult process(ExpiredStudyDto study) {
    // 1. Settlement (헤더) 생성
    Settlement settlement = Settlement.create(study.studyId());

    // 2. 각 참가자에 대해 SettlementItem 생성
    for (ParticipantDto participant : study.participants()) {
        SettlementItem item = SettlementItem.create(...);

        // 환급금 계산: 보증금 - (결석횟수 × 페널티)
        item.applyAttendanceResult(absenceCount, penaltyPerAbsence);
    }

    return SettlementCreationResult.of(settlement, items);
}
```

**환급금 계산 공식**:
```
환급금 = 보증금 - (결석 횟수 × 결석당 페널티)

예시:
- 보증금: 10,000원
- 결석 2회, 결석당 페널티 1,000원
- 환급금 = 10,000 - (2 × 1,000) = 8,000원
```

#### Writer: `SettlementWriter.java`

**역할**: Settlement과 SettlementItem을 DB에 저장합니다.

```java
@Override
public void write(Chunk<? extends SettlementCreationResult> chunk) {
    for (SettlementCreationResult result : chunk) {
        // 1. Settlement 저장 (ID 획득)
        Settlement saved = settlementRepository.save(result.settlement());

        // 2. SettlementItem에 settlementId 설정 후 저장
        for (SettlementItem item : result.items()) {
            setSettlementId(item, saved.getId());
        }
        settlementItemRepository.saveAll(items);
    }
}
```

### 4.2 Step 2: 정산 실행

#### Reader: `SettlementReader.java`

**역할**: PENDING 상태의 Settlement을 조회합니다.

```java
private void initialize() {
    // PENDING 또는 FAILED(재시도 가능) 상태 조회
    List<Settlement> settlementsToProcess =
        settlementRepository.findSettlementsToProcess(now);

    // 각 Settlement의 처리 대상 Item도 함께 로드
    List<SettlementExecutionDto> executionDtos = settlementsToProcess.stream()
        .map(settlement -> {
            List<SettlementItem> items =
                settlementItemRepository.findItemsToProcess(settlement.getId(), now);
            return SettlementExecutionDto.of(settlement, items);
        })
        .toList();
}
```

#### Processor: `ExecuteSettlementProcessor.java`

**역할**: 각 참가자에게 포인트를 지급합니다.

```java
@Override
public SettlementExecutionDto process(SettlementExecutionDto dto) {
    Settlement settlement = dto.settlement();

    // 1. Settlement 선점 (PENDING → PROCESSING)
    settlement.claim(now);

    // 2. 각 아이템에 대해 포인트 지급
    for (SettlementItem item : dto.itemsToProcess()) {
        // Member Service API 호출
        Long txId = memberRestClient.addPoints(
            item.getMemberId(),
            item.getRefundAmount(),
            "스터디 정산 환급"
        );
        item.markPaid(now, txId);
    }

    // 3. 모든 아이템 완료 시 Settlement 완료
    if (allCompleted) {
        settlement.complete(now);
    }
}
```

**상태 전이 흐름**:
```
Settlement: PENDING → PROCESSING → COMPLETED
                  ↘           ↗
                    FAILED (실패 시)
```

#### Writer: `ExecuteSettlementWriter.java`

**역할**: 최종 로깅만 담당 (저장은 Processor에서 이미 수행)

---

## 5. 데이터 흐름 다이어그램

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           Settlement Job                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────────── Step 1: 정산 생성 ──────────────────────┐     │
│  │                                                                │     │
│  │  ExpiredStudyReader          CreateSettlementProcessor         │     │
│  │  ┌─────────────────┐        ┌─────────────────────────┐       │     │
│  │  │ Study Service   │        │ ExpiredStudyDto         │       │     │
│  │  │ API 호출        │───────→│         ↓               │       │     │
│  │  │                 │        │ Settlement 생성         │       │     │
│  │  │ 완료된 스터디    │        │ SettlementItem 생성     │       │     │
│  │  │ 목록 조회       │        │ 환급금 계산              │       │     │
│  │  └─────────────────┘        └───────────┬─────────────┘       │     │
│  │                                         │                     │     │
│  │                             SettlementCreationResult          │     │
│  │                                         │                     │     │
│  │                                         ▼                     │     │
│  │                            SettlementWriter                   │     │
│  │                            ┌─────────────────────────┐        │     │
│  │                            │ DB 저장                 │        │     │
│  │                            │ - Settlement (PENDING)  │        │     │
│  │                            │ - SettlementItem        │        │     │
│  │                            └─────────────────────────┘        │     │
│  │                                                                │     │
│  └────────────────────────────────────────────────────────────────┘     │
│                                    │                                    │
│                                    ▼                                    │
│  ┌─────────────────────── Step 2: 정산 실행 ──────────────────────┐     │
│  │                                                                │     │
│  │  SettlementReader            ExecuteSettlementProcessor        │     │
│  │  ┌─────────────────┐        ┌─────────────────────────┐       │     │
│  │  │ PENDING 상태의   │        │ Settlement 선점         │       │     │
│  │  │ Settlement 조회  │───────→│ (PENDING → PROCESSING)  │       │     │
│  │  │                 │        │                         │       │     │
│  │  │ + 처리 대상     │        │ 각 Item에 대해:         │       │     │
│  │  │   Item 로드     │        │ - Member Service 호출   │       │     │
│  │  └─────────────────┘        │ - 포인트 지급           │       │     │
│  │                             │ - 상태 업데이트         │       │     │
│  │                             │                         │       │     │
│  │                             │ 완료 시:                │       │     │
│  │                             │ PROCESSING → COMPLETED  │       │     │
│  │                             └───────────┬─────────────┘       │     │
│  │                                         │                     │     │
│  │                                         ▼                     │     │
│  │                            ExecuteSettlementWriter            │     │
│  │                            ┌─────────────────────────┐        │     │
│  │                            │ 최종 로깅               │        │     │
│  │                            └─────────────────────────┘        │     │
│  │                                                                │     │
│  └────────────────────────────────────────────────────────────────┘     │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 6. 주요 설정 옵션

### 6.1 Chunk Size (묶음 크기)

**코드 위치**: `CreateSettlementStepConfig.java:34`

```java
.<ExpiredStudyDto, SettlementCreationResult>chunk(10, transactionManager)
```

- Step 1: `chunk(10)` - 10건씩 묶어서 처리
- Step 2: `chunk(5)` - 5건씩 묶어서 처리

**선택 기준**:
- 너무 작으면: 커밋 횟수 증가 → 성능 저하
- 너무 크면: 메모리 부담 + 실패 시 재처리 범위 증가

### 6.2 Skip 설정 (오류 허용)

```java
.faultTolerant()
.skipLimit(5)
.skip(Exception.class)
```

- `faultTolerant()`: 장애 허용 모드 활성화
- `skipLimit(5)`: 최대 5건까지 오류 허용
- `skip(Exception.class)`: Exception 발생 시 해당 건 스킵

**동작 방식**:
```
처리 중 오류 발생 → 해당 건 스킵 → 다음 건 처리 계속
                    (skipLimit 초과 시 전체 실패)
```

### 6.3 Listener (리스너)

배치 실행 전후에 특정 작업을 수행합니다.

**Job Listener** (`SettlementJobListener.java`):
```java
@Override
public void beforeJob(JobExecution jobExecution) {
    log.info("정산 배치 Job 시작");
}

@Override
public void afterJob(JobExecution jobExecution) {
    log.info("정산 배치 Job 종료");
    if (jobExecution.getStatus().isUnsuccessful()) {
        // TODO: Slack 알림 발송
    }
}
```

**Step Listener** (`CreateSettlementStepListener.java`):
- 처리 건수, 스킵 건수 등 상세 통계 로깅

### 6.4 재시도 메커니즘

실패 시 **지수 백오프(Exponential Backoff)**로 재시도합니다.

**코드 위치**: `ExecuteSettlementProcessor.java:138`

```java
// 지수 백오프: 2^n 분 후 재시도
LocalDateTime nextRetryAt = now.plusMinutes((long) Math.pow(2, nextRetryCount));
```

| 재시도 횟수 | 대기 시간 |
|------------|----------|
| 1회차 | 2분 후 |
| 2회차 | 4분 후 |
| 3회차 | 8분 후 |
| 초과 | 최종 실패 처리 |

---

## 7. 파일 구조 요약

```
payment-service/src/main/java/com/grow/payment/
├── adapter/
│   ├── batch/                          # Step 2: 정산 실행
│   │   ├── SettlementJobConfig.java    # Job 정의
│   │   ├── SettlementJobListener.java  # Job 리스너
│   │   ├── SettlementBatchScheduler.java # 스케줄러 (매일 3시)
│   │   ├── SettlementReader.java       # Step 2 Reader
│   │   ├── ExecuteSettlementProcessor.java # Step 2 Processor
│   │   ├── ExecuteSettlementWriter.java    # Step 2 Writer
│   │   ├── ExecuteSettlementStepConfig.java # Step 2 설정
│   │   ├── ExecuteSettlementStepListener.java # Step 2 리스너
│   │   └── SettlementExecutionDto.java # Step 2 DTO
│   │
│   └── config/                         # Step 1: 정산 생성
│       ├── BatchConfig.java            # Batch 기본 설정
│       ├── ExpiredStudyReader.java     # Step 1 Reader
│       ├── CreateSettlementProcessor.java # Step 1 Processor
│       ├── SettlementWriter.java       # Step 1 Writer
│       ├── CreateSettlementStepConfig.java # Step 1 설정
│       ├── CreateSettlementStepListener.java # Step 1 리스너
│       ├── ExpiredStudyDto.java        # Step 1 입력 DTO
│       └── SettlementCreationResult.java # Step 1 출력 DTO
│
└── domain/settlement/
    ├── Settlement.java                 # 정산 헤더 엔티티
    ├── SettlementItem.java             # 정산 항목 엔티티
    ├── SettlementStatus.java           # 정산 상태 enum
    └── SettlementItemStatus.java       # 항목 상태 enum
```

---

## 8. 테스트 작성 가이드

### 8.1 테스트 전략

배치 테스트는 **단위 테스트** 위주로 작성합니다. Reader, Processor의 비즈니스 로직을 독립적으로 검증합니다.

### 8.2 테스트 파일 구조

```
src/test/java/com/grow/payment/batch/
├── CreateSettlementStepTest.java      # Step 1 Reader/Processor 테스트
├── ExecuteSettlementStepTest.java     # Step 2 Reader/Processor 테스트
└── CreateSettlementProcessorTest.java # Processor 단위 테스트
```

### 8.3 테스트 예시

**Reader 테스트**:
```java
@ExtendWith(MockitoExtension.class)
class CreateSettlementStepTest {

    @Mock
    private StudyRestClient studyRestClient;

    @Mock
    private SettlementRepository settlementRepository;

    private ExpiredStudyReader reader;

    @BeforeEach
    void setUp() {
        reader = new ExpiredStudyReader(studyRestClient, settlementRepository);
    }

    @Test
    @DisplayName("Reader: 완료된 스터디가 있으면 ExpiredStudyDto를 반환한다")
    void readerShouldReturnExpiredStudyDtoWhenCompletedStudyExists() {
        // given
        when(studyRestClient.getCompletedStudiesForSettlement(anyInt()))
                .thenReturn(List.of(study));

        // when
        ExpiredStudyDto result = reader.read();

        // then
        assertThat(result).isNotNull();
    }
}
```

**Processor 테스트**:
```java
@Test
@DisplayName("Processor: 스터디 정보로 Settlement과 SettlementItem을 생성한다")
void processorShouldCreateSettlementAndItems() {
    // given
    ExpiredStudyDto study = new ExpiredStudyDto(1L, "스터디", 10000, 1000,
            List.of(new ParticipantDto(100L, 1L, 10000, 2, 8)));

    // when
    SettlementCreationResult result = processor.process(study);

    // then
    assertThat(result.settlement().getStudyId()).isEqualTo(1L);
    assertThat(result.items()).hasSize(1);
    assertThat(result.items().get(0).getRefundAmount()).isEqualTo(8000);
}
```

### 8.4 테스트 실행

```bash
# 모든 배치 테스트 실행
./gradlew test --tests "com.grow.payment.batch.*"

# 특정 테스트만 실행
./gradlew test --tests "CreateSettlementProcessorTest"
```

---

## 9. 정리

### Spring Batch 핵심 3가지

1. **Job → Step → Chunk** 구조로 대용량 처리
2. **Reader → Processor → Writer** 패턴으로 역할 분리
3. **Chunk 단위 트랜잭션**으로 안정성 확보

### 이 프로젝트에서 배울 수 있는 것

1. **MSA 환경에서의 배치**: Study Service, Member Service API 연동
2. **장애 대응**: Skip 설정, 재시도 메커니즘, 상태 관리
3. **모니터링**: Listener를 통한 로깅, 통계 수집
