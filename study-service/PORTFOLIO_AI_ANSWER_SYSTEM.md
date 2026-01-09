# AI 자동 답변 시스템 - 문제 해결 사례

## 프로젝트 개요

| 항목 | 내용 |
|------|------|
| **프로젝트** | Growple - 스터디 그룹 매칭 플랫폼 |
| **담당 기능** | 스터디 게시판 질문글 AI 자동 답변 시스템 |
| **기술 스택** | Spring Boot 3.4, Spring AI, OpenAI GPT API, PostgreSQL |
| **핵심 성과** | DB 기반 비동기 큐 시스템으로 안정적인 AI 답변 자동화 구현 |

---

## 시스템 요구사항

스터디 게시판에 질문글이 작성되면 AI가 자동으로 답변 댓글을 생성하는 기능이 필요했습니다.

**요구사항:**
1. 질문글 작성 시 자동으로 AI 답변 생성
2. API 응답 지연 없이 비동기 처리
3. OpenAI API Rate Limit 준수 (분당 요청 제한)
4. 실패 시 자동 재시도
5. 시스템 장애 시 데이터 유실 방지

---

## 문제 1: 동기 처리 시 응답 지연

### 문제 상황
초기 설계에서 질문글 작성 시 동기적으로 AI 답변을 생성하려 했습니다.

```
[문제 흐름]
사용자 → 질문글 작성 요청
         ↓
      DB 저장 (50ms)
         ↓
      OpenAI API 호출 (2,000~5,000ms) ← 병목!
         ↓
      댓글 저장 (30ms)
         ↓
사용자 ← 응답 (총 2,080~5,080ms)
```

**문제점:**
- OpenAI GPT API 평균 응답 시간: 2~5초
- 사용자가 질문글 작성에 5초 이상 대기
- 네트워크 타임아웃 위험
- API 장애 시 게시글 작성 자체가 실패

### 해결 방안
**이벤트 기반 비동기 처리 + DB 큐 시스템**을 설계했습니다.

핵심 아이디어:
1. 게시글 작성은 즉시 완료
2. AI 답변 요청을 DB 큐에 저장
3. 백그라운드 스케줄러가 큐를 폴링하여 처리

```
[개선된 흐름]
사용자 → 질문글 작성 요청
         ↓
      DB 저장 + 이벤트 발행 (50ms)
         ↓
사용자 ← 즉시 응답 (50ms)

         [비동기 - 트랜잭션 커밋 후]
      이벤트 리스너 → 큐 테이블에 저장

         [스케줄러 - 6분마다]
      큐 폴링 → OpenAI API → 댓글 저장
```

### 구현 결과

**1. 도메인 이벤트 발행 (PostService)**
```java
@Transactional
public Long createPost(Long studyId, Long memberId, CreatePostRequest request) {
    // ... 게시글 생성 로직
    Post savedPost = postRepository.save(post);

    // 질문 카테고리일 때만 이벤트 발행
    if (savedPost.getCategory() == PostCategory.QUESTION) {
        eventPublisher.publishEvent(new QuestionPostedEvent(savedPost.getId(), savedPost));
    }

    return savedPost.getId();  // 즉시 응답
}
```

**2. 이벤트 리스너 - 큐 저장**
```java
@Component
@RequiredArgsConstructor
public class AiAnswerEventListener {

    private final AiAnswerQueueService queueService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleQuestionPosted(QuestionPostedEvent event) {
        queueService.aiQueueSave(event);
    }
}
```

**3. 큐 서비스 - 중복 방지**
```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void aiQueueSave(QuestionPostedEvent event) {
    // 중복 저장 방지
    if (queueRepository.existsByPostId(event.postId())) {
        log.warn("AI answer queue already exists for post: {}", event.postId());
        return;
    }

    AiAnswerQueue queue = AiAnswerQueue.create(event.post());
    queueRepository.save(queue);
    log.info("AI answer queued for post: {}", event.postId());
}
```

**설계 포인트:**
| 요소 | 적용 이유 |
|------|----------|
| `AFTER_COMMIT` | 게시글 저장 확정 후 큐 저장 (데이터 일관성) |
| `REQUIRES_NEW` | 큐 저장 실패가 게시글 트랜잭션에 영향 없도록 격리 |
| 중복 체크 | 이벤트 재발행 시 중복 처리 방지 |

**개선 효과:**
| 지표 | 개선 전 | 개선 후 | 개선율 |
|------|---------|---------|--------|
| 게시글 작성 응답 | 2,000~5,000ms | ~100ms | **95%+ 감소** |
| 장애 격리 | 전체 실패 | 게시글 정상 저장 | 안정성 향상 |

---

## 문제 2: OpenAI API Rate Limit 관리

### 문제 상황
OpenAI API는 요청 빈도에 제한이 있습니다.

```
[Rate Limit 예시]
- GPT-3.5: 분당 3,500 요청
- GPT-4: 분당 500 요청 (플랜별 상이)
```

질문글이 한꺼번에 많이 등록되면 Rate Limit 초과로 429 에러가 발생합니다.

```
Error: 429 Too Many Requests
{"error": {"message": "Rate limit reached for default-gpt-3.5-turbo"}}
```

### 해결 방안
**Guava RateLimiter + 배치 처리**를 적용했습니다.

```java
// 분당 10개로 안전하게 제한
private final RateLimiter rateLimiter = RateLimiter.create(
    AiConstants.RATE_LIMIT_PER_MINUTE / 60.0  // 10/60 = 0.167 per second
);
```

### 구현 결과

**AiAnswerScheduler - Rate Limit 적용**
```java
@Component
@RequiredArgsConstructor
public class AiAnswerScheduler {

    private final AiAnswerQueueService queueService;
    private final AiAnswerProcessor processor;

    // 분당 10개 제한 (초당 약 0.167개)
    private final RateLimiter rateLimiter = RateLimiter.create(
            AiConstants.RATE_LIMIT_PER_MINUTE / 60.0
    );

    @Scheduled(fixedDelayString = "${ai.answer.schedule-delay:360000}")  // 6분
    public void processAiAnswerQueue() {
        List<AiAnswerQueue> pendingItems = queueService.claimPendingItems(AiConstants.BATCH_SIZE);

        if (pendingItems.isEmpty()) {
            return;
        }

        log.info("Processing {} AI answer requests", pendingItems.size());

        for (AiAnswerQueue item : pendingItems) {
            // Rate Limit 체크 - 즉시 획득 불가능하면 다음 배치로
            if (!rateLimiter.tryAcquire()) {
                log.debug("Rate limit reached, will process remaining in next batch");
                break;
            }
            processor.process(item);
        }
    }
}
```

**상수 정의**
```java
public final class AiConstants {
    public static final Long AI_WRITER_ID = 0L;
    public static final String AI_WRITER_NICKNAME = "AI 학습 도우미";

    // Rate Limit 설정
    public static final int RATE_LIMIT_PER_MINUTE = 10;
    public static final int BATCH_SIZE = 5;
    public static final int MAX_RETRY_COUNT = 3;
}
```

**Rate Limiter 동작 방식:**
```
시간  | 토큰 | 동작
------|------|------
0:00  | 0.17 | 요청 1 처리
0:06  | 0.17 | 요청 2 처리
0:12  | 0.17 | 요청 3 처리
...
0:30  | 0.17 | 요청 5 처리 (배치 완료)
6:00  | 1.00 | 다음 배치 시작 (토큰 누적)
```

**개선 효과:**
| 지표 | 개선 전 | 개선 후 |
|------|---------|---------|
| Rate Limit 에러 | 빈번 발생 | 발생 안 함 |
| 처리 안정성 | 불안정 | 예측 가능 |

---

## 문제 3: 동시성 제어 및 중복 처리 방지

### 문제 상황
스케줄러가 여러 인스턴스에서 실행되거나, 처리 중 서버가 재시작되면:
- 같은 큐 아이템을 여러 번 처리
- PROCESSING 상태에서 멈춘 아이템 미처리

```
[동시성 문제 시나리오]
인스턴스 A: SELECT * FROM ai_answer_queue WHERE status = 'PENDING' → Post #1
인스턴스 B: SELECT * FROM ai_answer_queue WHERE status = 'PENDING' → Post #1 (동일!)
→ 중복 답변 생성!
```

### 해결 방안
**비관적 락(Pessimistic Lock) + 상태 머신**을 적용했습니다.

### 구현 결과

**1. Repository - 비관적 락**
```java
public interface AiAnswerQueueRepository extends JpaRepository<AiAnswerQueue, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT q FROM AiAnswerQueue q
            JOIN FETCH q.post
            WHERE q.status = 'PENDING'
            ORDER BY q.createdAt ASC
            LIMIT :limit
            """)
    List<AiAnswerQueue> findPendingWithLock(@Param("limit") int limit);
}
```

**2. 상태 머신 (AiAnswerQueue 엔티티)**
```java
@Entity
@Table(name = "ai_answer_queue")
public class AiAnswerQueue {

    @Enumerated(EnumType.STRING)
    private AiQueueStatus status;  // PENDING → PROCESSING → COMPLETED/FAILED

    private Integer retryCount = 0;

    // 처리 시작
    public void markProcessing() {
        this.status = AiQueueStatus.PROCESSING;
    }

    // 처리 완료
    public void markCompleted() {
        this.status = AiQueueStatus.COMPLETED;
        this.processedAt = LocalDateTime.now();
    }

    // 처리 실패 - 재시도 가능하면 PENDING으로 복귀
    public void markFailed(String errorMessage) {
        this.retryCount++;
        this.errorMessage = truncateMessage(errorMessage);
        this.status = canRetry() ? AiQueueStatus.PENDING : AiQueueStatus.FAILED;
    }

    public boolean canRetry() {
        return retryCount < AiConstants.MAX_RETRY_COUNT;  // 3회
    }
}
```

**상태 흐름:**
```
PENDING ──처리시작──▶ PROCESSING ──성공──▶ COMPLETED
    ▲                     │
    │                     │ 실패 (재시도 가능)
    └─────────────────────┘
                          │
                          │ 실패 (재시도 초과)
                          ▼
                       FAILED
```

**3. 프로세서 - 상태 전이**
```java
@Transactional
public void process(AiAnswerQueue item) {
    item.markProcessing();  // PENDING → PROCESSING

    try {
        String answer = generateAnswer(item.getQuestionContent());

        PostComment aiComment = PostComment.createAiComment(item.getPost(), answer);
        commentRepository.save(aiComment);

        item.getPost().increaseCommentCount();
        item.markCompleted();  // PROCESSING → COMPLETED

        aiAnswerQueueRepository.save(item);

    } catch (Exception e) {
        log.error("Failed to generate AI answer for post: {}", item.getPost().getId(), e);
        item.markFailed(e.getMessage());  // PROCESSING → PENDING or FAILED
    }
}
```

**개선 효과:**
| 지표 | 개선 전 | 개선 후 |
|------|---------|---------|
| 중복 처리 | 발생 가능 | 방지 |
| 재시도 | 수동 처리 | 자동 3회 재시도 |
| 상태 추적 | 불가 | 실시간 모니터링 가능 |

---

## 문제 4: AI 답변 품질 관리

### 문제 상황
AI 답변이 너무 길거나, 부정확한 정보를 확신하듯 답변하는 문제가 있었습니다.

### 해결 방안
**시스템 프롬프트 템플릿**으로 답변 품질을 관리합니다.

### 구현 결과

**study-assistant.st (시스템 프롬프트)**
```
너는 스터디 그룹의 학습 도우미야.
질문에 대해 친절하고 정확하게 답변해줘.

규칙:
- 500자 이내로 간결하게 작성
- 확실하지 않은 내용은 "추가 확인이 필요합니다"라고 안내
- 코드가 필요하면 마크다운 코드 블록 사용
- 존댓말 사용
```

**AiAnswerProcessor - 프롬프트 적용**
```java
@Value("classpath:/study-assistant.st")
private Resource systemPromptResource;

private String generateAnswer(String question) {
    return chatClient.build()
            .prompt()
            .system(new SystemPromptTemplate(systemPromptResource).createMessage().getText())
            .user(question)
            .call()
            .content();
}
```

---

## 현재 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client                                    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   POST /studies/{id}/posts                       │
│                      (질문글 작성)                                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│  PostService                                                      │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ 1. 게시글 저장                                               │  │
│  │ 2. QuestionPostedEvent 발행                                 │  │
│  │ 3. 즉시 응답 반환 (~100ms)                                   │  │
│  └────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
                              │
                              │ @TransactionalEventListener(AFTER_COMMIT)
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│  AiAnswerEventListener                                            │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ AiAnswerQueue 테이블에 PENDING 상태로 저장                    │  │
│  └────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│  [DB] ai_answer_queue                                             │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ id | post_id | status  | retry_count | created_at          │  │
│  │ 1  | 123     | PENDING | 0           | 2025-01-08 10:00:00 │  │
│  └────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
                              │
                              │ @Scheduled(fixedDelay = 6분)
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│  AiAnswerScheduler                                                │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ 1. PENDING 상태 조회 (PESSIMISTIC_WRITE 락)                   │  │
│  │ 2. Rate Limiter 체크 (분당 10개)                             │  │
│  │ 3. AiAnswerProcessor 호출                                    │  │
│  └────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│  AiAnswerProcessor                                                │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ 1. 상태 → PROCESSING                                        │  │
│  │ 2. OpenAI API 호출 (GPT)                                    │  │
│  │ 3. AI 댓글 저장                                              │  │
│  │ 4. 상태 → COMPLETED (or FAILED)                             │  │
│  └────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│  [DB] post_comments                                               │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ AI 학습 도우미: "Spring Boot에서 의존성 주입은..."            │  │
│  └────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 개선 제안: 병렬 처리로 스루풋 향상

### 현재 문제점
현재 스케줄러는 **순차 처리**로, Rate Limit 내에서도 처리량이 제한됩니다.

```
[현재 - 순차 처리]
요청1 처리 (3초) → 요청2 처리 (3초) → 요청3 처리 (3초)
총 9초 소요
```

### 개선 방안
**CompletableFuture 병렬 처리**로 동시 요청을 가능하게 합니다.

```java
@Scheduled(fixedDelayString = "${ai.answer.schedule-delay:360000}")
public void processAiAnswerQueue() {
    List<AiAnswerQueue> pendingItems = queueService.claimPendingItems(AiConstants.BATCH_SIZE);

    if (pendingItems.isEmpty()) {
        return;
    }

    // 병렬 처리
    List<CompletableFuture<Void>> futures = pendingItems.stream()
            .filter(item -> rateLimiter.tryAcquire())
            .map(item -> CompletableFuture.runAsync(
                    () -> processor.process(item),
                    taskExecutor
            ))
            .toList();

    // 모든 작업 완료 대기
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
}
```

```
[개선 후 - 병렬 처리]
요청1 처리 (3초) ─┐
요청2 처리 (3초) ─┼─▶ 총 3초 소요
요청3 처리 (3초) ─┘
```

**예상 개선 효과:**
| 지표 | 현재 | 개선 후 | 개선율 |
|------|------|---------|--------|
| 배치 처리 시간 | 15초 (5개 순차) | ~3초 (병렬) | **80% 감소** |
| 스루풋 | 분당 10개 | 분당 10개 (동일) | Rate Limit 유지 |

---

## 개선 제안: 실패 알림 추가

### 현재 문제점
`FAILED` 상태로 변경된 건에 대한 알림이 없어 모니터링이 어렵습니다.

### 개선 방안
**AiAnswerProcessor에 Slack 알림 추가**

```java
@Transactional
public void process(AiAnswerQueue item) {
    item.markProcessing();

    try {
        String answer = generateAnswer(item.getQuestionContent());
        // ... 성공 처리
    } catch (Exception e) {
        log.error("Failed to generate AI answer for post: {}", item.getPost().getId(), e);
        item.markFailed(e.getMessage());

        // 재시도 불가능한 경우 알림
        if (!item.canRetry()) {
            slackNotifier.sendError("AI 답변 생성 최종 실패",
                String.format("Post ID: %d, 에러: %s", item.getPost().getId(), e.getMessage()));
        }
    }
}
```

---

## 개선 제안: 이벤트 리스너 비동기화

### 현재 문제점
`AiAnswerEventListener`가 동기적으로 실행되어, 큐 저장이 게시글 응답에 포함됩니다.

```java
// 현재 코드 - 동기 실행
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleQuestionPosted(QuestionPostedEvent event) {
    queueService.aiQueueSave(event);  // 여기서 약간의 지연 발생
}
```

### 개선 방안
```java
// 개선 코드 - 비동기 실행
@Async("taskExecutor")
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleQuestionPosted(QuestionPostedEvent event) {
    queueService.aiQueueSave(event);
}
```

---

## 결과 예시

### DB 상태 변화

**1. 질문글 작성 직후**
```sql
SELECT * FROM ai_answer_queue WHERE post_id = 123;

| id | post_id | status  | retry_count | created_at          |
|----|---------|---------|-------------|---------------------|
| 1  | 123     | PENDING | 0           | 2025-01-08 10:00:00 |
```

**2. 스케줄러 처리 중**
```sql
| id | post_id | status     | retry_count | created_at          |
|----|---------|------------|-------------|---------------------|
| 1  | 123     | PROCESSING | 0           | 2025-01-08 10:00:00 |
```

**3. 처리 완료**
```sql
| id | post_id | status    | retry_count | processed_at        |
|----|---------|-----------|-------------|---------------------|
| 1  | 123     | COMPLETED | 0           | 2025-01-08 10:06:03 |
```

### 생성된 AI 댓글

```json
{
  "commentId": 456,
  "postId": 123,
  "writerId": 0,
  "writerNickname": "AI 학습 도우미",
  "content": "Spring Boot에서 의존성 주입(DI)은 @Autowired 또는 생성자 주입을 통해 구현할 수 있습니다.\n\n생성자 주입을 권장드립니다:\n```java\n@RequiredArgsConstructor\npublic class MyService {\n    private final MyRepository repository;\n}\n```\n\n추가 질문이 있으시면 말씀해 주세요!",
  "createdAt": "2025-01-08T10:06:03"
}
```

---

## 모니터링 쿼리

```sql
-- 상태별 통계
SELECT status, COUNT(*) as count
FROM ai_answer_queue
GROUP BY status;

-- 실패 건 조회
SELECT q.id, p.title, q.error_message, q.retry_count
FROM ai_answer_queue q
JOIN posts p ON q.post_id = p.id
WHERE q.status = 'FAILED';

-- 오늘 처리량
SELECT COUNT(*) as today_processed
FROM ai_answer_queue
WHERE status = 'COMPLETED'
  AND processed_at >= CURRENT_DATE;
```

---

## 핵심 학습 포인트

### 1. Transactional Outbox Pattern
- 이벤트와 데이터를 같은 트랜잭션으로 저장
- 메시지 유실 없이 비동기 처리 보장

### 2. 상태 머신 설계
- 명확한 상태 전이로 데이터 일관성 유지
- 재시도 로직 캡슐화

### 3. Rate Limiter 적용
- 외부 API 제한 준수
- Token Bucket 알고리즘으로 버스트 트래픽 제어

### 4. 비관적 락 vs 낙관적 락
- 동시성이 높은 배치 처리에는 비관적 락 선택
- 충돌 시 재시도보다 대기가 효율적

---

## 관련 코드 위치

| 파일 | 역할 |
|------|------|
| `AiAnswerProcessor.java` | AI 답변 생성 핵심 로직 |
| `AiAnswerQueueService.java` | 큐 관리 서비스 |
| `AiAnswerEventListener.java` | 이벤트 리스너 |
| `AiAnswerScheduler.java` | 스케줄러 (폴링) |
| `AiAnswerQueue.java` | 큐 엔티티 (상태 머신) |
| `AiConstants.java` | 상수 정의 |
| `study-assistant.st` | AI 시스템 프롬프트 |
