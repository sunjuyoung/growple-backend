package com.grow.study.adapter.persistence;

import com.grow.study.application.required.SchedulerJobRepository;
import com.grow.study.domain.scheduler.JobType;
import com.grow.study.domain.scheduler.SchedulerJob;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SchedulerJobRepositoryAdapter implements SchedulerJobRepository {

    private final SchedulerJobJpaRepository jpaRepository;

    @Override
    public SchedulerJob save(SchedulerJob job) {
        return jpaRepository.save(job);
    }

    @Override
    public Optional<SchedulerJob> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<SchedulerJob> findClaimableJobs(JobType jobType, LocalDate scheduledDate, LocalDateTime now,   Pageable pageable) {
        return jpaRepository.findClaimableJobs(jobType, scheduledDate, now, pageable);
    }

    @Override
    public Optional<SchedulerJob> findByTypeAndTarget(JobType jobType, String targetType, Long targetId, LocalDate scheduledDate) {
        return jpaRepository.findByJobTypeAndTargetTypeAndTargetIdAndScheduledDate(jobType, targetType, targetId, scheduledDate);
    }

    @Override
    public List<SchedulerJob> findExhaustedJobs() {
        return jpaRepository.findExhaustedJobs();
    }
}
