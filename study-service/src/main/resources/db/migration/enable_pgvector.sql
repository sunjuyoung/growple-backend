-- pgvector 확장 활성화
-- PostgreSQL 16에서 pgvector 사용을 위한 설정

-- pgvector 확장 설치 (이미 설치되어 있으면 무시)
CREATE EXTENSION IF NOT EXISTS vector;

-- 확장 설치 확인
SELECT * FROM pg_extension WHERE extname = 'vector';

-- vector_store 테이블은 Spring AI가 자동으로 생성합니다.
-- application.yml의 spring.ai.vectorstore.pgvector.initialize-schema=true 설정으로 인해
-- 애플리케이션 시작 시 자동으로 아래 테이블이 생성됩니다:
--
-- CREATE TABLE vector_store (
--     id VARCHAR(255) PRIMARY KEY,
--     content TEXT,
--     metadata JSON,
--     embedding vector(1536)
-- );
--
-- CREATE INDEX vector_store_embedding_idx
-- ON vector_store USING hnsw (embedding vector_cosine_ops);
--
-- CREATE INDEX vector_store_metadata_idx
-- ON vector_store USING gin (metadata);

-- 기존 스터디들의 Document 일괄 생성은 별도 배치 작업으로 수행하세요.
