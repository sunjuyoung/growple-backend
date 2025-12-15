package com.grow.study.application;

import com.grow.common.DepositDeductionEvent;
import com.grow.study.adapter.persistence.AttendanceJpaRepository;
import com.grow.study.adapter.persistence.SessionJpaRepository;
import com.grow.study.adapter.persistence.StudyMemberJpaRepository;
import com.grow.study.application.required.StudyEventPublisher;
import com.grow.study.application.dto.AttendanceCheckResponse;
import com.grow.study.application.dto.AttendanceListResponse;
import com.grow.study.application.dto.SessionAttendanceResponse;
import com.grow.study.domain.study.Attendance;
import com.grow.study.domain.study.Session;
import com.grow.study.domain.study.StudyMember;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceService {

    private final AttendanceJpaRepository attendanceRepository;
    private final SessionJpaRepository sessionRepository;
    private final StudyMemberJpaRepository studyMemberRepository;
    private final StudyEventPublisher eventPublisher;

    /**
     * 출석 체크
     */
    public AttendanceCheckResponse checkAttendance(Long sessionId, Long studyId, Long memberId) {
        // 1. 세션 조회
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));


        // 2. 스터디 멤버 확인
        StudyMember studyMember = studyMemberRepository.findByStudyIdAndMemberId(
                studyId,
                memberId
        ).orElseThrow(() -> new IllegalArgumentException("스터디 멤버가 아닙니다."));

        // 3. 중복 출석 체크 방지
        if (attendanceRepository.existsBySessionIdAndMemberId(sessionId, memberId)) {
            throw new IllegalStateException("이미 출석 체크를 완료했습니다.");
        }

        // 4. 출석 체크 가능 시간 확인
        if (!session.isAttendanceCheckAvailable()) {
            if (session.isBeforeAttendanceCheck()) {
                throw new IllegalStateException("출석 체크 시작 전입니다.");
            } else if (session.isAfterAttendanceCheck()) {
                throw new IllegalStateException("출석 체크 시간이 종료되었습니다.");
            }
        }

        // 5. 출석 체크 생성
        Attendance attendance = Attendance.createPresent(session, memberId);
        Attendance savedAttendance = attendanceRepository.save(attendance);

        // 6. 세션 통계 업데이트
        session.incrementAttendance();

        // 7. 스터디 멤버 출석 통계 업데이트
        studyMember.recordAttendance(true);

        return AttendanceCheckResponse.from(savedAttendance);
    }

    /**
     * 회원의 출석 목록 조회
     */
    @Transactional(readOnly = true)
    public AttendanceListResponse getAttendanceList(Long studyId, Long memberId) {
        // 1. 스터디 멤버 확인
        studyMemberRepository.findByStudyIdAndMemberId(studyId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("스터디 멤버가 아닙니다."));

        // 2. 출석 목록 조회
        List<Attendance> attendances = attendanceRepository.findByStudyIdAndMemberId(studyId, memberId);

        // 3. 통계 계산
        AttendanceListResponse.AttendanceStatistics statistics =
                AttendanceListResponse.AttendanceStatistics.from(attendances);

        return AttendanceListResponse.of(attendances, statistics);
    }

    /**
     * 세션별 출석 현황 조회 (스터디장용)
     */
    @Transactional(readOnly = true)
    public SessionAttendanceResponse getSessionAttendance(Long sessionId, Long leaderId) {
        // 1. 세션 조회
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));

        // 2. 스터디장 권한 확인
        StudyMember studyMember = studyMemberRepository.findByStudyIdAndMemberId(
                session.getStudy().getId(),
                leaderId
        ).orElseThrow(() -> new IllegalArgumentException("스터디 멤버가 아닙니다."));

        if (!studyMember.isLeader()) {
            throw new IllegalStateException("스터디장만 조회할 수 있습니다.");
        }

        // 3. 세션의 출석 목록 조회
        List<Attendance> attendances = attendanceRepository.findBySessionId(sessionId);

        return SessionAttendanceResponse.of(session, attendances);
    }

    /**
     * 결석 처리 (자동 - 스케줄러에서 호출)
     */
    public void processAbsences(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));

        // 출석 체크 시간이 지났는지 확인
        if (!session.isAfterAttendanceCheck()) {
            throw new IllegalStateException("출석 체크 시간이 아직 종료되지 않았습니다.");
        }

        // 스터디의 모든 멤버 조회
        List<StudyMember> studyMembers = studyMemberRepository.findByStudyId(session.getStudy().getId());

        // 출석 체크하지 않은 멤버들을 결석 처리
        for (StudyMember member : studyMembers) {
            boolean hasChecked = attendanceRepository.existsBySessionIdAndMemberId(
                    sessionId,
                    member.getMemberId()
            );

            if (!hasChecked) {
                // 결석 처리
                Attendance absence = Attendance.createAbsent(session, member.getMemberId());
                attendanceRepository.save(absence);

                // 세션 통계 업데이트
                session.incrementAbsence();

                // 스터디 멤버 통계 업데이트
                member.recordAttendance(false);

                log.info("자동 결석 처리 - sessionId: {}, memberId: {}", sessionId, member.getMemberId());
            }
        }
    }

    /**
     * 결석 처리 + 보증금 차감 이벤트 발행 (스케줄러에서 호출)
     */
    public void processAbsencesWithDeduction(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));

        // 출석 체크 시간이 지났는지 확인
        if (!session.isAfterAttendanceCheck()) {
            throw new IllegalStateException("출석 체크 시간이 아직 종료되지 않았습니다.");
        }

        // 스터디의 ACTIVE 멤버 조회
        List<StudyMember> studyMembers = studyMemberRepository.findByStudyId(session.getStudy().getId());

        int absentCount = 0;

        for (StudyMember member : studyMembers) {
            boolean hasChecked = attendanceRepository.existsBySessionIdAndMemberId(
                    sessionId,
                    member.getMemberId()
            );

            if (!hasChecked) {
                // 결석 처리
                Attendance absence = Attendance.createAbsent(session, member.getMemberId());
                attendanceRepository.save(absence);

                // 세션 통계 업데이트
                session.incrementAbsence();

                // 스터디 멤버 통계 업데이트
                member.recordAttendance(false);

                // 보증금 차감
                member.deductDeposit();

                absentCount++;
                log.info("결석 처리 및 보증금 차감 - sessionId: {}, memberId: {}", sessionId, member.getMemberId());
            }
        }

        log.info("세션 결석 처리 완료 - sessionId: {}, 결석자 수: {}", sessionId, absentCount);
    }

    /**
     * 출석 상태 수정 (스터디장용)
     */
    public void updateAttendanceStatus(Long attendanceId, Long leaderId, String status, String note) {
        // 1. 출석 조회
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new IllegalArgumentException("출석 정보를 찾을 수 없습니다."));

        // 2. 스터디장 권한 확인
        StudyMember studyMember = studyMemberRepository.findByStudyIdAndMemberId(
                attendance.getSession().getStudy().getId(),
                leaderId
        ).orElseThrow(() -> new IllegalArgumentException("스터디 멤버가 아닙니다."));

        if (!studyMember.isLeader()) {
            throw new IllegalStateException("스터디장만 수정할 수 있습니다.");
        }

        // 3. 상태 변경
        attendance.updateStatus(
                com.grow.study.domain.study.AttendanceStatus.valueOf(status),
                note
        );

        log.info("출석 상태 수정 - attendanceId: {}, status: {}", attendanceId, status);
    }
}
