# pgvector 기반 스터디 추천 시스템 - 문제 해결 사례

## 프로젝트 개요

| 항목 | 내용 |
|------|------|
| **프로젝트** | Growple - 스터디 그룹 매칭 플랫폼 |
| **담당 기능** | AI 기반 스터디 추천 시스템 |
| **기술 스택** | Spring Boot 3.4, Spring AI 1.0.3, PostgreSQL 16 + pgvector, OpenAI Embedding API |
| **핵심 성과** | 벡터 유사도 기반 개인화 추천으로 스터디 매칭 정확도 향상 |

---

## 문제 1: 기존 키워드 검색의 한계

### 문제 상황
기존 스터디 검색은 단순 키워드 매칭 방식(LIKE 검색)을 사용했습니다.

```sql
-- 기존 검색 방식
SELECT * FROM study WHERE title LIKE '%스프링%' OR introduction LIKE '%스프링%';
```

**발생한 문제점:**
- "Spring Boot"를 검색하면 "스프링 부트"로 작성된 스터디가 검색되지 않음
- "백엔드 개발"을 검색해도 "서버 개발", "API 개발" 스터디가 누락
- 사용자 관심사와 유사한 스터디를 찾기 어려움
- 동의어, 유사어 처리 불가능

### 해결 방안
**시맨틱 검색(Semantic Search)** 도입을 결정했습니다.

텍스트를 의미 기반 벡터로 변환하여 코사인 유사도로 검색하면, 단어가 달라도 의미적으로 유사한 콘텐츠를 찾을 수 있습니다.

```
"Spring Boot 웹 개발" → [0.12, -0.34, 0.56, ...] (1536차원 벡터)
"스프링 부트 백엔드"  → [0.11, -0.33, 0.55, ...] (유사한 벡터값)
```

**기술 선택:**
| 선택지 | 장점 | 단점 | 결정 |
|--------|------|------|------|
| Elasticsearch | 풍부한 검색 기능 | 별도 인프라 필요, 비용 증가 | X |
| Pinecone | 관리형 서비스 | 외부 의존성, 비용 | X |
| **pgvector** | 기존 PostgreSQL 활용, 비용 절감 | 대규모 확장성 제한 | **O** |

### 구현 결과

```java
// StudyVectorService.java - 유사 스터디 검색
public List<Long> findSimilarStudies(Long studyId, int topK) {
    Study baseStudy = studyRepository.findById(studyId)
            .orElseThrow(() -> new IllegalArgumentException("Study not found"));

    String searchQuery = buildStudyContent(baseStudy);

    SearchRequest request = SearchRequest.builder()
            .query(searchQuery)
            .topK(topK + 1)
            .similarityThreshold(0.7)  // 70% 이상 유사도
            .filterExpression("status == 'RECRUITING'")
            .build();

    return vectorStore.similaritySearch(request).stream()
            .map(doc -> Long.parseLong(doc.getId()))
            .filter(id -> !id.equals(studyId))
            .limit(topK)
            .toList();
}
```

**개선 효과:**
| 지표 | 개선 전 | 개선 후 |
|------|---------|---------|
| 검색 정확도 | 키워드 일치만 가능 | 의미 유사도 기반 검색 |
| 동의어 처리 | 불가 | 자동 처리 (Spring = 스프링) |
| 추천 다양성 | 낮음 | 관련 스터디 폭넓게 추천 |

---

## 문제 2: API 응답 지연 문제

### 문제 상황
스터디 생성 시 벡터 임베딩을 동기적으로 처리하면서 심각한 응답 지연이 발생했습니다.

```
[문제 흐름]
사용자 → 스터디 생성 요청
         ↓
      DB 저장 (50ms)
         ↓
      OpenAI API 호출 (800~1500ms) ← 병목 지점!
         ↓
      pgvector 저장 (30ms)
         ↓
사용자 ← 응답 (총 1,380ms+)
```

**측정 결과:**
- OpenAI Embedding API 평균 응답 시간: 800~1,500ms
- 전체 스터디 생성 API 응답 시간: 1,380ms+
- 사용자 체감 지연으로 UX 저하

### 해결 방안
**이벤트 기반 비동기 처리** 패턴을 적용했습니다.

핵심 트랜잭션(스터디 생성)과 부가 작업(벡터 생성)을 분리하여 사용자 응답 속도를 개선합니다.

```
[개선된 흐름]
사용자 → 스터디 생성 요청
         ↓
      DB 저장 + 이벤트 발행 (50ms)
         ↓
사용자 ← 즉시 응답 (50ms)

         [별도 비동기 스레드]
      이벤트 수신 → OpenAI API → pgvector 저장
```

### 구현 결과

**1. 도메인 이벤트 발행 (StudyService)**
```java
@Transactional
public StudyResponse createStudy(CreateStudyCommand command, Long memberId) {
    Study study = Study.create(command, memberId);
    Study savedStudy = studyRepository.save(study);

    // 이벤트 발행 - 트랜잭션 내에서 발행, 커밋 후 처리
    eventPublisher.publishEvent(new StudyCreatedEvent(
        savedStudy.getId(),
        savedStudy.getTitle(),
        savedStudy.getIntroduction(),
        // ... 생략
    ));

    return StudyResponse.from(savedStudy);  // 즉시 응답
}
```

**2. 비동기 이벤트 리스너**
```java
@Slf4j
@Component
@RequiredArgsConstructor
public class StudyVectorEventListener {

    private final StudyVectorService studyVectorService;
    private final SlackNotifier slackNotifier;

    @Async("taskExecutor")  // 별도 스레드 풀에서 실행
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStudyCreatedEvent(StudyCreatedEvent event) {
        try {
            studyVectorService.createStudyDocument(event.studyId(), event);
            log.info("Vector document created for study: {}", event.studyId());
        } catch (Exception e) {
            log.error("Failed to create vector document: {}", event.studyId(), e);
            // 실패해도 스터디 생성에는 영향 없음
            slackNotifier.sendWarning("vector 생성실패",
                String.format("스터디 ID: %d, 오류: %s", event.studyId(), e.getMessage()));
        }
    }
}
```

**설계 포인트:**
| 요소 | 적용 이유 |
|------|----------|
| `@Async` | 메인 스레드 블로킹 방지 |
| `AFTER_COMMIT` | DB 커밋 확정 후 처리 (데이터 일관성) |
| try-catch | 벡터 생성 실패가 핵심 기능에 영향 없도록 격리 |
| Slack 알림 | 실패 시 즉시 모니터링 |

**개선 효과:**
| 지표 | 개선 전 | 개선 후 | 개선율 |
|------|---------|---------|--------|
| API 응답 시간 | 1,380ms+ | ~100ms | **92% 감소** |
| 사용자 대기 시간 | 체감 지연 | 즉시 응답 | 대폭 개선 |
| 장애 격리 | 전체 실패 | 부분 실패 허용 | 안정성 향상 |

---

## 문제 3: 벡터 검색 성능 최적화

### 문제 상황
스터디 데이터가 증가하면서 벡터 검색 성능이 저하될 우려가 있었습니다.

**벡터 검색의 특성:**
- 1536차원 벡터 간 코사인 유사도 계산 필요
- 브루트포스 방식: O(N) 시간 복잡도
- 10,000건 이상 시 100ms+ 예상

### 해결 방안
**HNSW(Hierarchical Navigable Small World) 인덱스**를 적용했습니다.

HNSW는 그래프 기반 근사 최근접 이웃(ANN) 알고리즘으로, 정확도를 약간 희생하고 검색 속도를 대폭 향상시킵니다.

```
[HNSW 작동 원리]
레벨 2:  A ─────────── B  (장거리 연결)
         │             │
레벨 1:  A ─── C ─── B    (중거리 연결)
         │    │    │
레벨 0:  A-D-C-E-B-F-G    (근거리 연결)

검색: 상위 레벨에서 대략적 위치 → 하위 레벨에서 정밀 탐색
```

### 구현 결과

**application.yml 설정:**
```yaml
spring:
  ai:
    vectorstore:
      pgvector:
        initialize-schema: true
        index-type: HNSW           # HNSW 인덱스 사용
        distance-type: COSINE_DISTANCE
        dimensions: 1536           # OpenAI embedding 차원

# 자동 생성되는 인덱스
# CREATE INDEX vector_store_embedding_idx
# ON vector_store USING hnsw (embedding vector_cosine_ops);
```

**메타데이터 필터링을 위한 GIN 인덱스:**
```sql
-- 상태, 카테고리 필터링 성능 향상
CREATE INDEX vector_store_metadata_idx ON vector_store USING gin (metadata);
```

**복합 필터 검색 예시:**
```java
// 카테고리 + 상태 필터 + 유사도 검색
SearchRequest request = SearchRequest.builder()
    .query(studyContent)
    .topK(10)
    .similarityThreshold(0.6)
    .filterExpression("category == 'PROGRAMMING' && status == 'RECRUITING'")
    .build();

// 실행 계획: GIN 인덱스로 필터링 → HNSW로 유사도 검색
```

**예상 성능:**
| 데이터 규모 | 브루트포스 | HNSW | 개선율 |
|-------------|-----------|------|--------|
| 1,000건 | ~10ms | ~2ms | 80% |
| 10,000건 | ~100ms | ~5ms | 95% |
| 100,000건 | ~1,000ms | ~10ms | 99% |

---

## 결과 예시

### API 요청/응답 예시

**1. 유사 스터디 추천**
```bash
GET /api/v1/studies/123/recommendations/similar?limit=5
```

```json
{
  "success": true,
  "data": [
    {
      "studyId": 456,
      "title": "스프링 부트 심화 스터디",
      "category": "PROGRAMMING",
      "level": "INTERMEDIATE",
      "status": "RECRUITING",
      "currentParticipants": 5,
      "maxParticipants": 10,
      "similarity": 0.87
    },
    {
      "studyId": 789,
      "title": "Java/Spring 취업 준비반",
      "category": "PROGRAMMING",
      "level": "BEGINNER",
      "status": "RECRUITING",
      "currentParticipants": 3,
      "maxParticipants": 8,
      "similarity": 0.82
    }
  ]
}
```

**2. 사용자 관심사 기반 추천**
```bash
POST /api/v1/studies/recommendations/by-interest
Content-Type: application/json

{
  "memberIntroduction": "백엔드 개발에 관심이 많습니다. 특히 Spring Boot와 JPA를 깊이 공부하고 싶습니다.",
  "limit": 10
}
```

```json
{
  "success": true,
  "data": [
    {
      "studyId": 101,
      "title": "JPA 완전 정복",
      "introduction": "JPA/Hibernate 심화 학습...",
      "category": "PROGRAMMING",
      "similarity": 0.91
    },
    {
      "studyId": 102,
      "title": "Spring Boot 프로젝트 스터디",
      "introduction": "실전 프로젝트로 배우는 스프링...",
      "category": "PROGRAMMING",
      "similarity": 0.85
    }
  ]
}
```

---

## 아키텍처 다이어그램

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client                                    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     API Gateway                                  │
└─────────────────────────────────────────────────────────────────┘
                              │
          ┌───────────────────┴───────────────────┐
          ▼                                       ▼
┌──────────────────┐                    ┌──────────────────┐
│   스터디 생성     │                    │   추천 조회       │
│   POST /studies  │                    │ GET /recommendations│
└──────────────────┘                    └──────────────────┘
          │                                       │
          ▼                                       ▼
┌──────────────────┐                    ┌──────────────────┐
│  StudyService    │                    │ Recommendation   │
│  (트랜잭션)       │                    │    Service       │
└──────────────────┘                    └──────────────────┘
          │                                       │
          │ ① DB 저장                              │ ③ 벡터 검색
          ▼                                       ▼
┌──────────────────┐                    ┌──────────────────┐
│   PostgreSQL     │                    │ StudyVectorService│
│   (Study 테이블)  │                    │                  │
└──────────────────┘                    └──────────────────┘
          │                                       │
          │ ② 이벤트 발행                          │
          ▼                                       ▼
┌──────────────────┐                    ┌──────────────────┐
│ EventListener    │──────────────────▶│    VectorStore   │
│ (비동기, @Async) │     임베딩 저장     │   (pgvector)     │
└──────────────────┘                    └──────────────────┘
          │                                       ▲
          ▼                                       │
┌──────────────────┐                              │
│   OpenAI API     │──────────────────────────────┘
│ (text-embedding) │     1536차원 벡터
└──────────────────┘
```

---

## 핵심 학습 포인트

### 1. 기술적 의사결정
- **pgvector 선택**: 기존 인프라 활용으로 비용 절감, 트랜잭션 일관성 유지
- **Spring AI 활용**: 벡터 스토어 추상화로 확장성 확보

### 2. 성능 최적화
- **비동기 처리**: 이벤트 기반 아키텍처로 응답 시간 92% 개선
- **HNSW 인덱스**: O(log N) 검색으로 확장성 확보

### 3. 장애 대응
- **장애 격리**: 벡터 생성 실패가 핵심 기능에 영향 없도록 설계
- **모니터링**: Slack 알림으로 실패 즉시 감지

---

## 관련 코드 위치

| 파일 | 역할 |
|------|------|
| `StudyVectorService.java` | 벡터 검색 핵심 로직 |
| `StudyRecommendationService.java` | 추천 비즈니스 로직 |
| `StudyVectorEventListener.java` | 비동기 이벤트 처리 |
| `StudyVectorBatchService.java` | 기존 데이터 마이그레이션 |
| `StudyRecommendationApi.java` | REST API 컨트롤러 |
