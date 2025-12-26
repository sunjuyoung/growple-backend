package com.grow.study.adapter.scheduler;

import com.grow.study.application.required.SchedulerJobRepository;
import com.grow.study.domain.scheduler.SchedulerJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 실패한 Job 알림 스케줄러
 * - 매일 09:00 (KST)에 실행
 * - 재시도 횟수를 초과한 Job을 조회하여 로그 경고
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FailedJobAlertScheduler {

    private final SchedulerJobRepository jobRepository;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void alertFailedJobs() {
        log.info("실패 Job 알림 스케줄러 시작");

        List<SchedulerJob> exhaustedJobs = jobRepository.findExhaustedJobs();

        if (exhaustedJobs.isEmpty()) {
            log.info("재시도 초과 Job 없음");
            return;
        }

        log.warn("=== 재시도 초과 Job 알림 ({} 건) ===", exhaustedJobs.size());

        for (SchedulerJob job : exhaustedJobs) {
            log.warn("JobId: {}, Type: {}, Target: {}:{}, ScheduledDate: {}, RetryCount: {}, Error: {}",
                    job.getId(),
                    job.getJobType(),
                    job.getTargetType(),
                    job.getTargetId(),
                    job.getScheduledDate(),
                    job.getRetryCount(),
                    job.getLastError()
            );
        }

        // TODO: 향후 Slack Webhook 연동
        // slackNotificationService.sendAlert("재시도 초과 Job " + exhaustedJobs.size() + "건 발견");

        log.info("실패 Job 알림 스케줄러 완료");
    }
}
