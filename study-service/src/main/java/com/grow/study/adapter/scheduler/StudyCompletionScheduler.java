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

    private final StudyRepository studyRepository;

    @Scheduled(cron = "0 20 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void processStudyCompletion() {
        log.info("스터디 종료 처리 스케줄러 시작");

        LocalDate today = LocalDate.now();
        List<Study> targetStudies = studyRepository.findByStatusAndEndDateBefore(
                StudyStatus.IN_PROGRESS,
                today
        );

        if (targetStudies.isEmpty()) {
            log.info("처리할 스터디 종료 대상 없음");
            return;
        }

        log.info("스터디 종료 대상 {}개 처리 시작", targetStudies.size());

        int completedCount = 0;

        for (Study study : targetStudies) {
            try {
                study.complete();
                completedCount++;
                log.info("스터디 종료 완료 - studyId: {}, title: {}, participants: {}",
                        study.getId(), study.getTitle(), study.getCurrentParticipants());

                // TODO: Settlement 생성 (PENDING 상태)
                // settlementService.createSettlement(study);

            } catch (Exception e) {
                log.error("스터디 종료 처리 실패 - studyId: {}, error: {}",
                        study.getId(), e.getMessage(), e);
            }
        }

        log.info("스터디 종료 처리 스케줄러 완료 - 종료: {}개", completedCount);
    }
}
