-- =====================================================
-- scheduler_job 테이블 생성
-- 멱등성/재시도 전략을 위한 Job 관리 테이블
-- =====================================================

CREATE TABLE scheduler_job (
    id                      BIGSERIAL PRIMARY KEY,
    job_type                VARCHAR(30) NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    target_type             VARCHAR(20) NOT NULL,
    target_id               BIGINT NOT NULL,
    scheduled_date          DATE NOT NULL,
    processing_started_at   TIMESTAMP,
    retry_count             INT NOT NULL DEFAULT 0,
    max_retries             INT NOT NULL DEFAULT 3,
    next_retry_at           TIMESTAMP,
    last_error              VARCHAR(1000),
    completed_at            TIMESTAMP,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version                 BIGINT NOT NULL DEFAULT 0
);

-- 주석
COMMENT ON TABLE scheduler_job IS '스케줄러 Job 관리 테이블';
COMMENT ON COLUMN scheduler_job.id IS 'Job ID';
COMMENT ON COLUMN scheduler_job.job_type IS 'Job 타입 (RECRUITMENT_DEADLINE, STUDY_START, STUDY_COMPLETION, ATTENDANCE_PROCESS)';
COMMENT ON COLUMN scheduler_job.status IS 'Job 상태 (PENDING, PROCESSING, COMPLETED, FAILED)';
COMMENT ON COLUMN scheduler_job.target_type IS '대상 타입 (STUDY, SESSION)';
COMMENT ON COLUMN scheduler_job.target_id IS '대상 ID (studyId 또는 sessionId)';
COMMENT ON COLUMN scheduler_job.scheduled_date IS '예정 처리일';
COMMENT ON COLUMN scheduler_job.processing_started_at IS '처리 시작 시간 (Claim 시점)';
COMMENT ON COLUMN scheduler_job.retry_count IS '현재 재시도 횟수';
COMMENT ON COLUMN scheduler_job.max_retries IS '최대 재시도 횟수';
COMMENT ON COLUMN scheduler_job.next_retry_at IS '다음 재시도 예정 시간';
COMMENT ON COLUMN scheduler_job.last_error IS '마지막 에러 메시지';
COMMENT ON COLUMN scheduler_job.completed_at IS '완료 시간';
COMMENT ON COLUMN scheduler_job.created_at IS '생성 시간';
COMMENT ON COLUMN scheduler_job.version IS '낙관적 락 버전';

-- 인덱스
CREATE INDEX idx_job_type_status ON scheduler_job (job_type, status, next_retry_at);
CREATE INDEX idx_job_target ON scheduler_job (target_type, target_id);
CREATE INDEX idx_job_scheduled_date ON scheduler_job (scheduled_date);

-- 유니크 제약조건 (멱등성 보장)
CREATE UNIQUE INDEX uk_job_target ON scheduler_job (job_type, target_type, target_id, scheduled_date);
