package com.grow.study.adapter.scheduler;

import com.grow.study.application.required.StudyRepository;
import com.grow.study.domain.study.Study;
import com.grow.study.domain.study.StudyStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

    private final StudyRepository studyRepository;

    @Scheduled(cron = "0 10 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void processStudyStart() {
        log.info("스터디 시작 처리 스케줄러 시작");

        LocalDate today = LocalDate.now();
        List<Study> targetStudies = studyRepository.findByStatusAndStartDate(
                StudyStatus.RECRUIT_CLOSED,
                today
        );

        if (targetStudies.isEmpty()) {
            log.info("처리할 스터디 시작 대상 없음");
            return;
        }

        log.info("스터디 시작 대상 {}개 처리 시작", targetStudies.size());

        int startedCount = 0;

        for (Study study : targetStudies) {
            try {
                study.start();
                startedCount++;
                log.info("스터디 시작 완료 - studyId: {}, title: {}, participants: {}",
                        study.getId(), study.getTitle(), study.getCurrentParticipants());
            } catch (Exception e) {
                log.error("스터디 시작 처리 실패 - studyId: {}, error: {}",
                        study.getId(), e.getMessage(), e);
            }
        }

        log.info("스터디 시작 처리 스케줄러 완료 - 시작: {}개", startedCount);
    }
}
