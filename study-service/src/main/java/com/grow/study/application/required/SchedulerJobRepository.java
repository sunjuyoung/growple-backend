package com.grow.study.application.required;

import com.grow.study.domain.scheduler.JobType;
import com.grow.study.domain.scheduler.SchedulerJob;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SchedulerJobRepository {

    SchedulerJob save(SchedulerJob job);

    Optional<SchedulerJob> findById(Long id);

    /**
     * 처리 가능한 Job 조회 (PENDING 또는 FAILED 상태, 재시도 시간 도래)
     */
    List<SchedulerJob> findClaimableJobs(JobType jobType, LocalDate scheduledDate, LocalDateTime now,   Pageable pageable);

    /**
     * 특정 대상의 Job 조회 (중복 방지용)
     */
    Optional<SchedulerJob> findByTypeAndTarget(JobType jobType, String targetType, Long targetId, LocalDate scheduledDate);

    /**
     * 재시도 횟수 초과된 Job 조회
     */
    List<SchedulerJob> findExhaustedJobs();
}
