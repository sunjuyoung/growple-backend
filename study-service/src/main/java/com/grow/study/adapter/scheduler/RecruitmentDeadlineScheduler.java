package com.grow.study.adapter.scheduler;

import com.grow.study.adapter.intergration.SlackNotifier;
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
 * 모집 마감 처리 스케줄러
 * - 매일 00:05 (KST)에 실행
 * - RECRUITING 상태이면서 모집 종료일(recruitEndDate)이 오늘인 스터디 대상
 * - 최소 인원 미달 시: CANCELLED
 * - 최소 인원 충족 시: RECRUIT_CLOSED
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecruitmentDeadlineScheduler {

    private static final int BATCH_SIZE = 100;
    private static final JobType JOB_TYPE = JobType.RECRUITMENT_DEADLINE;

    private final SchedulerJobRepository jobRepository;
    private final StudyRepository studyRepository;
    private final SlackNotifier slackNotifier;

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void processRecruitmentDeadline() {
        log.info("모집 마감 처리 스케줄러 시작");

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        List<SchedulerJob> jobs = jobRepository.findClaimableJobs(JOB_TYPE, today, now, PageRequest.of(0, BATCH_SIZE));

        if (jobs.isEmpty()) {
            log.info("처리할 모집 마감 Job 없음");
            return;
        }

        log.info("모집 마감 Job {}개 처리 시작", jobs.size());

        int closedCount = 0;
        int cancelledCount = 0;
        int failedCount = 0;

        for (SchedulerJob job : jobs) {
            try {
                job.claim(now);
                jobRepository.save(job);

                Study study = studyRepository.findById(job.getTargetId())
                        .orElseThrow(() -> new IllegalStateException("Study not found: " + job.getTargetId()));

                if (study.hasMinimumParticipants()) {
                    study.closeRecruitment();
                    closedCount++;
                    log.info("스터디 모집 마감 완료 - studyId: {}, title: {}, participants: {}/{}",
                            study.getId(), study.getTitle(),
                            study.getCurrentParticipants(), study.getMinParticipants());
                    studyRepository.save(study);
                } else {
                    study.cancel();
                    cancelledCount++;
                    studyRepository.save(study);
                    log.info("스터디 최소 인원 미달 취소 - studyId: {}, title: {}, participants: {}/{}",
                            study.getId(), study.getTitle(),
                            study.getCurrentParticipants(), study.getMinParticipants());
                    // TODO: 참가자 결제 전액 포인트 환급 처리
                }

                job.complete(now);
                jobRepository.save(job);

            } catch (Exception e) {
                log.error("모집 마감 처리 실패 - jobId: {}, studyId: {}, error: {}",
                        job.getId(), job.getTargetId(), e.getMessage(), e);
                job.fail(e.getMessage(), calculateNextRetry(job.getRetryCount()));
                jobRepository.save(job);
                failedCount++;
            }
        }

        slackNotifier.sendInfo("스터디 모집 마감 처리 스케줄러 완료",
                String.format("모집 마감 처리 완료 - 마감: %d개, 취소: %d개, 실패: %d개",
                        closedCount, cancelledCount, failedCount));
        log.info("모집 마감 처리 스케줄러 완료 - 마감: {}개, 취소: {}개, 실패: {}개",
                closedCount, cancelledCount, failedCount);
    }

    private LocalDateTime calculateNextRetry(int currentRetryCount) {
        int delayMinutes = 5 * (int) Math.pow(3, currentRetryCount);
        return LocalDateTime.now().plusMinutes(delayMinutes);
    }
}
