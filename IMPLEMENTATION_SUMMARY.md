# pgvector 기반 RAG 스터디 추천 시스템 구현 완료

## 구현 개요
Growple 백엔드에 PostgreSQL pgvector를 활용한 RAG(Retrieval Augmented Generation) 기반 스터디 추천 시스템을 성공적으로 구현했습니다.

## 구현된 기능

### 1. 유사 스터디 추천
- **API**: `GET /api/v1/studies/{studyId}/recommendations/similar`
- **기능**: 스터디 내용(제목, 소개, 커리큘럼) 기반 유사 스터디 검색
- **필터**: 모집 중(`RECRUITING`) 스터디만 반환
- **유사도**: 70% 이상

### 2. 멤버 관심사 기반 매칭
- **API**: `POST /api/v1/studies/recommendations/by-interest`
- **기능**: 멤버의 관심사(소개) 기반 스터디 추천
- **필터**: 모집 중 스터디만 반환
- **유사도**: 60% 이상

### 3. 카테고리별 유사 스터디 검색
- **API**: `GET /api/v1/studies/recommendations/by-category`
- **기능**: 카테고리 내에서 검색 텍스트와 유사한 스터디 추천
- **필터**: 카테고리 + 모집 중
- **유사도**: 60% 이상

### 4. 이벤트 기반 자동 Document 생성
- 스터디 생성 시 **자동으로 pgvector Document 생성**
- 비동기 처리로 API 응답 속도에 영향 없음
- 트랜잭션 커밋 후 실행으로 데이터 일관성 보장
- **스터디 수정은 지원하지 않음** (요구사항에 따라)

### 5. 배치 작업 (관리자)
- **API**: `POST /api/v1/admin/studies/vector/batch/create-recruiting`
- **기능**: 기존 모집 중인 스터디들의 Document 일괄 생성
- **용도**: 초기 마이그레이션, 데이터 동기화

## 기술 스택

### 의존성 추가 (build.gradle)
```gradle
// pgvector
implementation 'com.pgvector:pgvector:0.1.4'

// Spring AI
implementation 'org.springframework.ai:spring-ai-openai-spring-boot-starter'
implementation 'org.springframework.ai:spring-ai-starter-vector-store-pgvector'
```

### 설정 (application.yml)
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
`.env` 파일에 추가 필요:
```properties
OPENAI_API_KEY=sk-...
```

## 아키텍처

### 이벤트 기반 Document 생성 흐름
```
스터디 생성 API 호출
    ↓
StudyService.register()
    ↓
Study 엔티티 저장
    ↓
StudyCreatedEvent 발행 (ApplicationEventPublisher)
    ↓
StudyVectorEventListener (비동기, AFTER_COMMIT)
    ↓
StudyVectorService.createStudyDocument()
    ↓
OpenAI Embedding API 호출 (text-embedding-3-small)
    ↓
pgvector 저장 (VectorStore)
```

### 검색 흐름
```
추천 API 호출
    ↓
StudyRecommendationService
    ↓
StudyVectorService.findSimilarStudies()
    ↓
VectorStore.similaritySearch()
    ↓
pgvector HNSW 인덱스 검색
    ↓
메타데이터 필터 적용 (status, category)
    ↓
Study 엔티티 조회 및 반환
```

## 생성된 파일

### 도메인 이벤트
- `StudyCreatedEvent.java` - 스터디 생성 이벤트 (record)

### 서비스 계층
- `StudyVectorService.java` - 벡터 검색 및 Document 관리
- `StudyRecommendationService.java` - 추천 비즈니스 로직
- `StudyVectorBatchService.java` - 배치 작업 (일괄 생성)

### 어댑터 계층
- `StudyVectorEventListener.java` - 이벤트 리스너 (비동기)
- `StudyRecommendationApi.java` - 추천 REST API
- `StudyVectorBatchApi.java` - 배치 작업 API (관리자)

### DTO
- `StudyRecommendationResponse.java` - 추천 응답 DTO
- `MemberInterestRequest.java` - 관심사 요청 DTO

### 설정
- `AsyncConfig.java` - 비동기 처리 설정 (@EnableAsync)

### 문서
- `VECTOR_STORE_README.md` - 상세 사용 가이드
- `enable_pgvector.sql` - pgvector 확장 활성화 스크립트

## 수정된 기존 파일

### StudyService.java
- `ApplicationEventPublisher` 의존성 추가
- `register()` 메서드에서 스터디 저장 후 `StudyCreatedEvent` 발행

### Study.java (도메인 엔티티)
- 이벤트 발행 코드 제거 (ID 없이 발행하면 안 되므로)
- StudyService에서 저장 후 발행하도록 변경

### build.gradle
- Spring AI BOM 추가
- pgvector, Spring AI 의존성 추가
- Maven milestone 리포지토리 추가

### application.yml
- Spring AI 설정 추가 (OpenAI, pgvector)

## 데이터베이스 스키마

Spring AI가 자동 생성하는 테이블:
```sql
CREATE TABLE vector_store (
    id VARCHAR(255) PRIMARY KEY,           -- 스터디 ID
    content TEXT,                          -- 스터디 내용 (임베딩 원본)
    metadata JSON,                         -- 메타데이터
    embedding vector(1536)                 -- 임베딩 벡터
);

-- HNSW 인덱스 (빠른 유사도 검색)
CREATE INDEX vector_store_embedding_idx
ON vector_store USING hnsw (embedding vector_cosine_ops);

-- 메타데이터 인덱스 (필터링)
CREATE INDEX vector_store_metadata_idx
ON vector_store USING gin (metadata);
```

## 메타데이터 필터링

Document 생성 시 스터디 상태 포함:
```json
{
  "studyId": 123,
  "title": "스프링 부트 스터디",
  "category": "PROGRAMMING",
  "level": "INTERMEDIATE",
  "status": "RECRUITING",      // 필터링에 사용
  "minParticipants": 3,
  "maxParticipants": 10,
  "startDate": "2025-02-01",
  "endDate": "2025-04-30"
}
```

검색 시 필터 적용:
```java
SearchRequest.query(content)
    .withTopK(10)
    .withSimilarityThreshold(0.7)
    .withFilterExpression("status == 'RECRUITING'");
```

## API 엔드포인트

### 추천 API
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/studies/{studyId}/recommendations/similar` | 유사 스터디 추천 |
| POST | `/api/v1/studies/recommendations/by-interest` | 멤버 관심사 기반 추천 |
| GET | `/api/v1/studies/recommendations/by-category` | 카테고리별 유사 스터디 추천 |

### 관리자 API
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/admin/studies/vector/batch/create-recruiting` | 모집 중인 스터디 Document 일괄 생성 |
| POST | `/api/v1/admin/studies/vector/batch/recreate-all` | 전체 Document 재생성 |

## 사용 예시

### 1. 스터디 생성
```bash
POST /api/v1/studies
# 자동으로 벡터 Document 생성됨 (비동기)
```

### 2. 유사 스터디 추천
```bash
curl -X GET "http://localhost:8082/api/v1/studies/123/recommendations/similar?limit=5"
```

### 3. 멤버 관심사 기반 추천
```bash
curl -X POST "http://localhost:8082/api/v1/studies/recommendations/by-interest" \
  -H "Content-Type: application/json" \
  -d '{
    "memberIntroduction": "백엔드 개발에 관심이 많고 스프링을 공부하고 싶습니다.",
    "limit": 10
  }'
```

### 4. 기존 스터디 Document 일괄 생성 (관리자)
```bash
curl -X POST "http://localhost:8082/api/v1/admin/studies/vector/batch/create-recruiting"
```

## 배포 가이드

### 1. 환경 변수 설정
`.env` 파일에 추가:
```properties
OPENAI_API_KEY=sk-proj-...
```

### 2. pgvector 확장 활성화
PostgreSQL에 pgvector 확장 설치:
```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

### 3. 애플리케이션 시작
```bash
cd study-service
./gradlew bootRun
```

Spring AI가 자동으로 `vector_store` 테이블과 인덱스를 생성합니다.

### 4. 기존 스터디 Document 생성 (선택사항)
```bash
curl -X POST "http://localhost:8082/api/v1/admin/studies/vector/batch/create-recruiting"
```

## 성능 최적화

1. **HNSW 인덱스**: O(log N) 시간 복잡도로 빠른 유사도 검색
2. **GIN 인덱스**: 메타데이터 필터링 성능 향상
3. **비동기 Document 생성**: API 응답 속도에 영향 없음
4. **코사인 유사도**: 텍스트 유사도에 최적화된 거리 메트릭

## 모니터링

### 로그 확인
```
# Document 생성
Creating vector document for study: 123
Successfully created vector document for study: 123

# 검색
Finding similar studies for studyId: 123, topK: 5

# 오류
Failed to create vector document for study: 123
```

### 데이터 확인
```sql
-- Document 개수
SELECT COUNT(*) FROM vector_store;

-- 모집 중인 스터디 Document
SELECT id, metadata->>'title', metadata->>'status'
FROM vector_store
WHERE metadata->>'status' = 'RECRUITING';
```

## 향후 개선 사항

1. **재시도 로직**: Document 생성 실패 시 자동 재시도
2. **스터디 수정 지원**: 스터디 정보 변경 시 Document 업데이트
3. **스터디 삭제 처리**: 스터디 삭제 시 Document 자동 삭제
4. **하이브리드 검색**: 키워드 검색 + 벡터 검색 결합
5. **개인화 추천**: 멤버 학습 이력 기반 추천 강화
6. **A/B 테스트**: 추천 알고리즘 성능 측정

## 주요 설계 결정

### 1. 스터디 수정 시 Document 미업데이트
- **이유**: 요구사항에서 수정은 없는 걸로 명시
- **향후**: 필요 시 `StudyUpdatedEvent` 추가 가능

### 2. 비동기 이벤트 처리
- **이유**: API 응답 속도 보장, OpenAI API 호출 시간 분리
- **장점**: 스터디 생성 API 성능에 영향 없음
- **단점**: 즉시 검색 불가 (수 초 후 검색 가능)

### 3. AFTER_COMMIT 이벤트 리스너
- **이유**: 데이터 일관성 보장
- **장점**: 트랜잭션 롤백 시 이벤트 미발행
- **단점**: 트랜잭션 외부에서 실행 (에러 처리 독립적)

### 4. 메타데이터 필터링
- **이유**: 모집 중인 스터디만 추천
- **구현**: `status == 'RECRUITING'` 필터
- **성능**: GIN 인덱스로 빠른 필터링

## 참고 문서

- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [pgvector GitHub](https://github.com/pgvector/pgvector)
- [OpenAI Embeddings Guide](https://platform.openai.com/docs/guides/embeddings)
- [HNSW Algorithm](https://arxiv.org/abs/1603.09320)

## 구현 완료 체크리스트

- [x] pgvector 의존성 추가
- [x] Spring AI 설정
- [x] 도메인 이벤트 생성
- [x] 이벤트 리스너 구현 (비동기, AFTER_COMMIT)
- [x] VectorService 구현
- [x] RecommendationService 구현
- [x] REST API 구현
- [x] 배치 작업 구현
- [x] DTO 생성
- [x] 문서 작성
- [x] 메타데이터 필터링 (status)
- [x] 비동기 설정 (@EnableAsync)

## 테스트 시나리오

### 1. 스터디 생성 및 Document 자동 생성
```bash
# 1. 스터디 생성
POST /api/v1/studies

# 2. 로그 확인 (비동기 처리)
# Creating vector document for study: {id}
# Vector document created successfully

# 3. DB 확인
SELECT * FROM vector_store WHERE id = '{study_id}';
```

### 2. 유사 스터디 추천
```bash
# 1. 유사 스터디 조회
GET /api/v1/studies/123/recommendations/similar?limit=5

# 2. 응답 확인 (모집 중인 스터디만 반환)
# status: "RECRUITING"
```

### 3. 멤버 관심사 기반 추천
```bash
# 1. 관심사 기반 추천
POST /api/v1/studies/recommendations/by-interest
{
  "memberIntroduction": "자바 백엔드 개발 공부하고 싶습니다",
  "limit": 10
}

# 2. 응답 확인 (관련 스터디 추천)
```

## 완료!

pgvector 기반 RAG 스터디 추천 시스템 구현이 완료되었습니다.
상세 사용 가이드는 `study-service/VECTOR_STORE_README.md`를 참고하세요.
