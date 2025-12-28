package com.grow.study.adapter.scheduler;

import com.grow.study.application.required.SchedulerJobRepository;
import com.grow.study.application.required.StudyRepository;
import com.grow.study.domain.scheduler.JobType;
import com.grow.study.domain.scheduler.SchedulerJob;
import com.grow.study.domain.study.Study;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 스터디 시작 처리 스케줄러
 * - 매일 00:10 (KST)에 실행
 * - RECRUIT_CLOSED 상태이면서 시작일(startDate)이 오늘인 스터디 대상
 * - IN_PROGRESS로 상태 전환
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StudyStartScheduler {

    private static final int BATCH_SIZE = 100;
    private static final JobType JOB_TYPE = JobType.STUDY_START;

    private final SchedulerJobRepository jobRepository;
    private final StudyRepository studyRepository;

    @Scheduled(cron = "0 10 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void processStudyStart() {
        log.info("스터디 시작 처리 스케줄러 시작");

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        List<SchedulerJob> jobs = jobRepository.findClaimableJobs(JOB_TYPE, today, now, PageRequest.of(0, BATCH_SIZE));

        if (jobs.isEmpty()) {
            log.info("처리할 스터디 시작 Job 없음");
            return;
        }

        log.info("스터디 시작 Job {}개 처리 시작", jobs.size());

        int startedCount = 0;
        int failedCount = 0;

        for (SchedulerJob job : jobs) {
            try {
                job.claim(now);
                jobRepository.save(job);

                Study study = studyRepository.findById(job.getTargetId())
                        .orElseThrow(() -> new IllegalStateException("Study not found: " + job.getTargetId()));

                study.start();
                startedCount++;
                log.info("스터디 시작 완료 - studyId: {}, title: {}, participants: {}",
                        study.getId(), study.getTitle(), study.getCurrentParticipants());
                studyRepository.save(study);
                job.complete(now);
                jobRepository.save(job);

            } catch (OptimisticLockingFailureException e) {
                log.debug("Job 선점 실패 (다른 인스턴스가 처리 중) - jobId: {}", job.getId());
                continue;
            } catch (Exception e) {
                log.error("스터디 시작 처리 실패 - jobId: {}, studyId: {}, error: {}",
                        job.getId(), job.getTargetId(), e.getMessage(), e);
                job.fail(e.getMessage(), calculateNextRetry(job.getRetryCount()));
                jobRepository.save(job);
                failedCount++;
            }
        }

        log.info("스터디 시작 처리 스케줄러 완료 - 시작: {}개, 실패: {}개", startedCount, failedCount);
    }

    private LocalDateTime calculateNextRetry(int currentRetryCount) {
        int delayMinutes = 5 * (int) Math.pow(3, currentRetryCount);
        return LocalDateTime.now().plusMinutes(delayMinutes);
    }
}
