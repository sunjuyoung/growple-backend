-- =====================================================
-- 기존 데이터를 scheduler_job으로 마이그레이션
-- 현재 진행 중인 스터디에 대한 Job 생성
-- =====================================================

-- 1. RECRUITING 상태 스터디 → RECRUITMENT_DEADLINE Job 생성
INSERT INTO scheduler_job (job_type, status, target_type, target_id, scheduled_date, created_at)
SELECT
    'RECRUITMENT_DEADLINE',
    'PENDING',
    'STUDY',
    id,
    recruit_end_date,
    CURRENT_TIMESTAMP
FROM studies
WHERE status = 'RECRUITING'
  AND recruit_end_date >= CURRENT_DATE
ON CONFLICT (job_type, target_type, target_id, scheduled_date) DO NOTHING;

-- 2. RECRUIT_CLOSED 상태 스터디 → STUDY_START Job 생성
INSERT INTO scheduler_job (job_type, status, target_type, target_id, scheduled_date, created_at)
SELECT
    'STUDY_START',
    'PENDING',
    'STUDY',
    id,
    start_date,
    CURRENT_TIMESTAMP
FROM studies
WHERE status = 'RECRUIT_CLOSED'
  AND start_date >= CURRENT_DATE
ON CONFLICT (job_type, target_type, target_id, scheduled_date) DO NOTHING;

-- 3. IN_PROGRESS 상태 스터디 → STUDY_COMPLETION Job 생성
INSERT INTO scheduler_job (job_type, status, target_type, target_id, scheduled_date, created_at)
SELECT
    'STUDY_COMPLETION',
    'PENDING',
    'STUDY',
    id,
    end_date,
    CURRENT_TIMESTAMP
FROM studies
WHERE status = 'IN_PROGRESS'
  AND end_date >= CURRENT_DATE
ON CONFLICT (job_type, target_type, target_id, scheduled_date) DO NOTHING;
