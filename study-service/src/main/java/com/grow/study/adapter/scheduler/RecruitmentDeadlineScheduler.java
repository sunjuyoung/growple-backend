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

    private final StudyRepository studyRepository;

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void processRecruitmentDeadline() {
        log.info("모집 마감 처리 스케줄러 시작");

        LocalDate today = LocalDate.now();
        List<Study> targetStudies = studyRepository.findByStatusAndRecruitEndDate(
                StudyStatus.RECRUITING,
                today
        );

        if (targetStudies.isEmpty()) {
            log.info("처리할 모집 마감 대상 스터디 없음");
            return;
        }

        log.info("모집 마감 대상 스터디 {}개 처리 시작", targetStudies.size());

        int cancelledCount = 0;
        int closedCount = 0;

        for (Study study : targetStudies) {
            try {
                if (study.hasMinimumParticipants()) {
                    study.closeRecruitment();
                    closedCount++;
                    log.info("스터디 모집 마감 완료 - studyId: {}, title: {}, participants: {}/{}",
                            study.getId(), study.getTitle(),
                            study.getCurrentParticipants(), study.getMinParticipants());
                } else {
                    study.cancel();
                    cancelledCount++;
                    log.info("스터디 최소 인원 미달 취소 - studyId: {}, title: {}, participants: {}/{}",
                            study.getId(), study.getTitle(),
                            study.getCurrentParticipants(), study.getMinParticipants());
                    // TODO: 참가자 결제 전액 포인트 환급 처리
                }
            } catch (Exception e) {
                log.error("스터디 모집 마감 처리 실패 - studyId: {}, error: {}",
                        study.getId(), e.getMessage(), e);
            }
        }

        log.info("모집 마감 처리 스케줄러 완료 - 마감: {}개, 취소: {}개", closedCount, cancelledCount);
    }
}
