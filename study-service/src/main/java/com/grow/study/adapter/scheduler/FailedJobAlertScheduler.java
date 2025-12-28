package com.grow.study.adapter.scheduler;

import com.grow.study.adapter.intergration.SlackNotifier;
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
    private final SlackNotifier slackNotifier;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void alertFailedJobs() {
        log.info("실패 Job 알림 스케줄러 시작");

        List<SchedulerJob> exhaustedJobs = jobRepository.findExhaustedJobs();

        if (exhaustedJobs.isEmpty()) {
            log.info("재시도 초과 Job 없음");
            return;
        }

        // TODO: 향후 Slack Webhook 연동
        slackNotifier.sendInfo("재시도 초과 Job " + exhaustedJobs.size() + "건 발견",
                 String.format("재시도 초과 Job %d건이 발견되었습니다. 자세한 내용은 로그를 확인해주세요.", exhaustedJobs.size()));

        log.info("실패 Job 알림 스케줄러 완료");
    }
}
