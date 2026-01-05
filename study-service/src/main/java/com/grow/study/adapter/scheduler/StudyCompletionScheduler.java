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
 * 스터디 종료 처리 스케줄러
 * - 매일 00:20 (KST)에 실행
 * - IN_PROGRESS 상태이면서 종료일(endDate)이 오늘 이전인 스터디 대상
 * - COMPLETED로 상태 전환
 * - Settlement 생성 (TODO)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StudyCompletionScheduler {

    private static final int BATCH_SIZE = 100;
    private static final JobType JOB_TYPE = JobType.STUDY_COMPLETION;

    private final SchedulerJobRepository jobRepository;
    private final StudyRepository studyRepository;

    @Scheduled(cron = "0 */9 * * * *", zone = "Asia/Seoul")
    @Transactional
    public void processStudyCompletion() {
        log.info("스터디 종료 처리 스케줄러 시작");

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        List<SchedulerJob> jobs = jobRepository.findClaimableJobs(JOB_TYPE, today, now, PageRequest.of(0, BATCH_SIZE));

        if (jobs.isEmpty()) {
            log.info("처리할 스터디 종료 Job 없음");
            return;
        }

        log.info("스터디 종료 Job {}개 처리 시작", jobs.size());

        int completedCount = 0;
        int failedCount = 0;

        for (SchedulerJob job : jobs) {
            try {
                job.claim(now);
                jobRepository.save(job);

                Study study = studyRepository.findById(job.getTargetId())
                        .orElseThrow(() -> new IllegalStateException("Study not found: " + job.getTargetId()));

                study.complete();
                completedCount++;
                studyRepository.save(study);
                log.info("스터디 종료 완료 - studyId: {}, title: {}, participants: {}",
                        study.getId(), study.getTitle(), study.getCurrentParticipants());

                // TODO: Settlement 생성 kafka 이벤트 발행

                job.complete(now);
                jobRepository.save(job);

            } catch (OptimisticLockingFailureException e) {
                log.debug("Job 선점 실패 (다른 인스턴스가 처리 중) - jobId: {}", job.getId());
                continue;
            } catch (Exception e) {
                log.error("스터디 종료 처리 실패 - jobId: {}, studyId: {}, error: {}",
                        job.getId(), job.getTargetId(), e.getMessage(), e);
                job.fail(e.getMessage(), calculateNextRetry(job.getRetryCount()));
                jobRepository.save(job);
                failedCount++;
            }
        }

        log.info("스터디 종료 처리 스케줄러 완료 - 종료: {}개, 실패: {}개", completedCount, failedCount);
    }

    private LocalDateTime calculateNextRetry(int currentRetryCount) {
        int delayMinutes = 5 * (int) Math.pow(3, currentRetryCount);
        return LocalDateTime.now().plusMinutes(delayMinutes);
    }
}
