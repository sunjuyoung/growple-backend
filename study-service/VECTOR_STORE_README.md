# pgvector 기반 스터디 추천 시스템

## 개요
PostgreSQL pgvector 확장을 활용한 RAG(Retrieval Augmented Generation) 기반 스터디 추천 시스템입니다.
스터디 내용 및 멤버 관심사를 벡터화하여 유사도 기반 추천을 제공합니다.

## 기술 스택
- **Vector Database**: PostgreSQL 16 + pgvector 0.1.4
- **Embedding Model**: OpenAI text-embedding-3-small (1536 dimensions)
- **Framework**: Spring AI 1.0.3
- **Vector Index**: HNSW (Hierarchical Navigable Small World)
- **Distance Metric**: Cosine Distance

## 주요 기능

### 1. 유사 스터디 추천
스터디의 제목, 소개, 커리큘럼, 스터디장 메시지를 기반으로 유사한 스터디를 추천합니다.

**API Endpoint:**
```http
GET /api/v1/studies/{studyId}/recommendations/similar?limit=5
```

**특징:**
- 스터디 내용을 벡터화하여 유사도 70% 이상인 스터디 검색
- 모집 중(`RECRUITING`) 상태의 스터디만 반환
- 자기 자신은 결과에서 제외

### 2. 멤버 관심사 기반 스터디 매칭
멤버의 소개(관심사)를 기반으로 적합한 스터디를 추천합니다.

**API Endpoint:**
```http
POST /api/v1/studies/recommendations/by-interest
Content-Type: application/json

{
  "memberIntroduction": "자바와 스프링을 공부하고 싶습니다. 백엔드 개발에 관심이 많습니다.",
  "limit": 10
}
```

**특징:**
- 유사도 60% 이상인 스터디 검색
- 모집 중인 스터디만 반환

### 3. 카테고리별 유사 스터디 검색
특정 카테고리 내에서 검색 텍스트와 유사한 스터디를 추천합니다.

**API Endpoint:**
```http
GET /api/v1/studies/recommendations/by-category?query=Spring Boot&category=PROGRAMMING&limit=10
```

**특징:**
- 카테고리 필터링 + 유사도 검색
- 유사도 60% 이상
- 모집 중인 스터디만 반환

## 아키텍처

### 이벤트 기반 Document 생성
```
스터디 생성 (StudyService)
    ↓
Study 엔티티 저장
    ↓
StudyCreatedEvent 발행 (ApplicationEventPublisher)
    ↓
StudyVectorEventListener (비동기)
    ↓
StudyVectorService.createStudyDocument()
    ↓
OpenAI Embedding API 호출
    ↓
pgvector 저장 (VectorStore)
```

### 계층 구조
```
adapter/webapi/
  ├── StudyRecommendationApi.java          # REST API 컨트롤러
  └── dto/
      ├── StudyRecommendationResponse.java # 추천 응답 DTO
      └── MemberInterestRequest.java       # 관심사 요청 DTO

adapter/integration/
  └── StudyVectorEventListener.java        # 이벤트 리스너 (비동기)

application/
  ├── StudyRecommendationService.java      # 추천 서비스
  ├── StudyVectorService.java              # 벡터 검색 서비스
  └── StudyService.java                    # 스터디 생성 (이벤트 발행)

domain/event/
  └── StudyCreatedEvent.java               # 도메인 이벤트

adapter/config/
  └── AsyncConfig.java                     # 비동기 설정
```

## Document 구조

### Content (임베딩 대상)
스터디 정보를 하나의 텍스트로 결합하여 임베딩합니다:

```
제목: {title}
소개: {introduction}
커리큘럼: {curriculum}
스터디장 메시지: {leaderMessage}
카테고리: {category}
난이도: {level}
```

### Metadata (필터링)
벡터 검색 시 필터링을 위한 메타데이터:

```json
{
  "studyId": 123,
  "title": "스프링 부트 스터디",
  "category": "PROGRAMMING",
  "level": "INTERMEDIATE",
  "status": "RECRUITING",
  "minParticipants": 3,
  "maxParticipants": 10,
  "startDate": "2025-02-01",
  "endDate": "2025-04-30"
}
```

## 설정

### application.yml
```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      embedding:
        options:
          model: text-embedding-3-small
    vectorstore:
      pgvector:
        initialize-schema: true
        index-type: HNSW
        distance-type: COSINE_DISTANCE
        dimensions: 1536
```

### 환경 변수
`.env` 파일에 추가:
```properties
OPENAI_API_KEY=sk-...
```

## 데이터베이스 스키마

Spring AI가 자동으로 생성하는 테이블:

```sql
CREATE TABLE IF NOT EXISTS vector_store (
    id VARCHAR(255) PRIMARY KEY,           -- 스터디 ID
    content TEXT,                          -- 스터디 내용 (임베딩 원본)
    metadata JSON,                         -- 메타데이터 (필터링용)
    embedding vector(1536)                 -- 임베딩 벡터
);

-- HNSW 인덱스 (빠른 유사도 검색)
CREATE INDEX IF NOT EXISTS vector_store_embedding_idx
ON vector_store USING hnsw (embedding vector_cosine_ops);

-- 메타데이터 인덱스 (필터링 성능 향상)
CREATE INDEX IF NOT EXISTS vector_store_metadata_idx
ON vector_store USING gin (metadata);
```

## 이벤트 처리 흐름

### 1. 스터디 생성 시
```java
// StudyService.java:98-118
Study savedStudy = studyRepository.save(study);

// 이벤트 발행
eventPublisher.publishEvent(new StudyCreatedEvent(
    savedStudy.getId(),
    savedStudy.getTitle(),
    savedStudy.getIntroduction(),
    savedStudy.getCurriculum(),
    savedStudy.getLeaderMessage(),
    savedStudy.getCategory(),
    savedStudy.getLevel(),
    savedStudy.getStatus(),
    savedStudy.getMinParticipants(),
    savedStudy.getMaxParticipants(),
    savedStudy.getSchedule().getStartDate(),
    savedStudy.getSchedule().getEndDate()
));
```

### 2. 이벤트 리스너
```java
// StudyVectorEventListener.java
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleStudyCreatedEvent(StudyCreatedEvent event) {
    studyVectorService.createStudyDocument(event.studyId(), event);
}
```

**특징:**
- `@Async`: 비동기 처리 (스터디 생성 API 응답 속도에 영향 없음)
- `AFTER_COMMIT`: 트랜잭션 커밋 후 실행 (데이터 일관성 보장)
- 실패 시 스터디 생성에 영향 없음 (로그만 기록)

### 3. Document 생성
```java
// StudyVectorService.java
public void createStudyDocument(Long studyId, StudyCreatedEvent event) {
    String content = buildStudyContent(event);           // 텍스트 결합
    Map<String, Object> metadata = buildMetadata(...);   // 메타데이터 생성
    Document document = new Document(studyId, content, metadata);
    vectorStore.add(List.of(document));                  // 벡터 저장
}
```

## 검색 알고리즘

### 유사도 검색 흐름
1. 검색 텍스트를 OpenAI API로 임베딩 (1536차원 벡터)
2. pgvector HNSW 인덱스로 코사인 유사도 계산
3. 메타데이터 필터 적용 (`status == 'RECRUITING'`)
4. 유사도 임계값 적용 (0.6 ~ 0.7)
5. Top-K 결과 반환

### 성능 최적화
- **HNSW 인덱스**: O(log N) 시간 복잡도로 빠른 검색
- **메타데이터 필터**: GIN 인덱스로 효율적인 필터링
- **비동기 Document 생성**: API 응답 속도 영향 없음

## 사용 예시

### 1. 스터디 생성 시 자동 Document 생성
```java
// 스터디 생성 API 호출
POST /api/v1/studies

// 자동으로 벡터 Document 생성됨 (비동기)
// 로그 확인:
// Creating vector document for study: 123
// Vector document created successfully for study: 123
```

### 2. 유사 스터디 추천
```bash
curl -X GET "http://localhost:8082/api/v1/studies/123/recommendations/similar?limit=5"
```

**응답:**
```json
[
  {
    "studyId": 456,
    "title": "스프링 부트 심화",
    "thumbnailUrl": "https://...",
    "category": "PROGRAMMING",
    "level": "INTERMEDIATE",
    "status": "RECRUITING",
    "minParticipants": 3,
    "maxParticipants": 10,
    "currentParticipants": 5,
    "depositAmount": 10000,
    "startDate": "2025-02-15",
    "endDate": "2025-05-15",
    "introduction": "스프링 부트 실전 프로젝트..."
  }
]
```

### 3. 멤버 관심사 기반 추천
```bash
curl -X POST "http://localhost:8082/api/v1/studies/recommendations/by-interest" \
  -H "Content-Type: application/json" \
  -d '{
    "memberIntroduction": "백엔드 개발을 공부하고 싶습니다. 특히 스프링 프레임워크와 JPA에 관심이 많습니다.",
    "limit": 10
  }'
```

## 모니터링

### 로그 확인
```
# Document 생성
Creating vector document for study: 123
Successfully created vector document for study: 123

# 검색
Finding similar studies for studyId: 123, topK: 5
Recommending studies based on member interest, limit: 10

# 오류
Failed to create vector document for study: 123
```

### 데이터 확인
```sql
-- Document 개수
SELECT COUNT(*) FROM vector_store;

-- 최근 생성된 Document
SELECT id, metadata->>'title', metadata->>'status'
FROM vector_store
ORDER BY id DESC
LIMIT 10;

-- 모집 중인 스터디 Document
SELECT COUNT(*)
FROM vector_store
WHERE metadata->>'status' = 'RECRUITING';
```

## 향후 개선 사항

1. **재시도 로직**: Document 생성 실패 시 재시도 메커니즘
2. **스터디 수정**: 스터디 정보 수정 시 Document 업데이트
3. **스터디 삭제**: 스터디 삭제 시 Document 삭제
4. **배치 처리**: 기존 스터디들의 Document 일괄 생성
5. **하이브리드 검색**: 키워드 검색 + 벡터 검색 결합
6. **개인화 추천**: 멤버 학습 이력 기반 추천

## 트러블슈팅

### Document가 생성되지 않는 경우
1. OpenAI API 키 확인: `OPENAI_API_KEY` 환경 변수
2. 비동기 설정 확인: `@EnableAsync` 활성화
3. 로그 확인: 이벤트 리스너 실행 여부
4. pgvector 확장 설치 확인:
   ```sql
   CREATE EXTENSION IF NOT EXISTS vector;
   ```

### 검색 결과가 없는 경우
1. 모집 중인 스터디 확인: `status == 'RECRUITING'`
2. 유사도 임계값 조정: `withSimilarityThreshold()` 값 낮추기
3. Document 존재 여부 확인: `SELECT * FROM vector_store;`
4. 메타데이터 필터 확인: 카테고리, 상태 등

## 관련 파일

### 핵심 파일
- `StudyVectorService.java:study-service/src/main/java/com/grow/study/application/StudyVectorService.java` - 벡터 검색 서비스
- `StudyRecommendationService.java:study-service/src/main/java/com/grow/study/application/StudyRecommendationService.java` - 추천 서비스
- `StudyVectorEventListener.java:study-service/src/main/java/com/grow/study/adapter/intergration/StudyVectorEventListener.java` - 이벤트 리스너
- `StudyRecommendationApi.java:study-service/src/main/java/com/grow/study/adapter/webapi/StudyRecommendationApi.java` - REST API

### 설정 파일
- `build.gradle:study-service/build.gradle` - 의존성 설정
- `application.yml:study-service/src/main/resources/application.yml` - Spring AI 설정
- `AsyncConfig.java:study-service/src/main/java/com/grow/study/adapter/config/AsyncConfig.java` - 비동기 설정

### 이벤트 파일
- `StudyCreatedEvent.java:study-service/src/main/java/com/grow/study/domain/event/StudyCreatedEvent.java` - 도메인 이벤트
- `StudyService.java:study-service/src/main/java/com/grow/study/application/StudyService.java` - 이벤트 발행
