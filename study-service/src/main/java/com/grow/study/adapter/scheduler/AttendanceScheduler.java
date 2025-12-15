package com.grow.study.adapter.scheduler;

import com.grow.study.adapter.persistence.SessionJpaRepository;
import com.grow.study.application.AttendanceService;
import com.grow.study.domain.study.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 출석 관련 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceScheduler {

    private final SessionJpaRepository sessionRepository;
    private final AttendanceService attendanceService;

    /**
     * 마감된 세션의 결석 처리 및 보증금 차감
     * - 5분마다 실행
     * - 출석 마감 시간이 지난 세션 조회
     * - 출석 체크하지 않은 멤버 결석 처리
     * - 보증금 차감 이벤트 발행
     */
    @Scheduled(cron = "0 */20 * * * *")
    @Transactional
    public void processExpiredSessions() {
        log.info("출석 마감 처리 스케줄러 시작");

        LocalDateTime now = LocalDateTime.now();
        List<Session> expiredSessions = sessionRepository.findExpiredAndUnprocessed(now);

        if (expiredSessions.isEmpty()) {
            log.debug("처리할 마감 세션 없음");
            return;
        }

        log.info("마감 세션 {}개 처리 시작", expiredSessions.size());

        for (Session session : expiredSessions) {
            try {
                processSession(session);
            } catch (Exception e) {
                log.error("세션 처리 실패 - sessionId: {}, error: {}", session.getId(), e.getMessage(), e);
                // 개별 세션 실패해도 다른 세션 계속 처리
            }
        }

        log.info("출석 마감 처리 스케줄러 완료");
    }

    private void processSession(Session session) {
        log.info("세션 결석 처리 시작 - sessionId: {}, studyId: {}, sessionNumber: {}",
                session.getId(), session.getStudy().getId(), session.getSessionNumber());

        // 결석 처리 + 보증금 차감 이벤트 발행
        attendanceService.processAbsencesWithDeduction(session.getId());

        // 처리 완료 마킹
        session.markAttendanceProcessed();

        log.info("세션 결석 처리 완료 - sessionId: {}", session.getId());
    }
}
