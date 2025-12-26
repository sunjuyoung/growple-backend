package com.grow.study.application;

import com.grow.study.application.required.SchedulerJobRepository;
import com.grow.study.domain.scheduler.JobType;
import com.grow.study.domain.scheduler.SchedulerJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerJobService {

    private final SchedulerJobRepository jobRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createJobIfNotExists(JobType jobType, String targetType,
                                     Long targetId, LocalDate scheduledDate) {
        if (scheduledDate == null) {
            return;
        }

        boolean exists = jobRepository
                .findByTypeAndTarget(jobType, targetType, targetId, scheduledDate)
                .isPresent();

        if (exists) {
            return;
        }

        SchedulerJob job = SchedulerJob.create(jobType, targetType, targetId, scheduledDate);
        jobRepository.save(job);

        log.info("Job 생성 완료 - jobType: {}, targetType: {}, targetId: {}, scheduledDate: {}",
                jobType, targetType, targetId, scheduledDate);
    }
}
