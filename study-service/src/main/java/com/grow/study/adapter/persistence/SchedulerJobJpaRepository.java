package com.grow.study.adapter.persistence;

import com.grow.study.domain.scheduler.JobType;
import com.grow.study.domain.scheduler.SchedulerJob;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SchedulerJobJpaRepository extends JpaRepository<SchedulerJob, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT j FROM SchedulerJob j
            WHERE j.jobType = :jobType
              AND j.scheduledDate <= :scheduledDate
              AND j.status IN (com.grow.study.domain.scheduler.JobStatus.PENDING, com.grow.study.domain.scheduler.JobStatus.FAILED)
              AND (j.nextRetryAt IS NULL OR j.nextRetryAt <= :now)
              AND j.retryCount < j.maxRetries
            ORDER BY j.scheduledDate, j.id
            """)
    List<SchedulerJob> findClaimableJobs(
            @Param("jobType") JobType jobType,
            @Param("scheduledDate") LocalDate scheduledDate,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    Optional<SchedulerJob> findByJobTypeAndTargetTypeAndTargetIdAndScheduledDate(
            JobType jobType,
            String targetType,
            Long targetId,
            LocalDate scheduledDate
    );

    @Query("""
            SELECT j FROM SchedulerJob j
            WHERE j.status = com.grow.study.domain.scheduler.JobStatus.FAILED
              AND j.retryCount >= j.maxRetries
            ORDER BY j.scheduledDate DESC
            """)
    List<SchedulerJob> findExhaustedJobs();
}
