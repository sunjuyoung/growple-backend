package com.grow.study.application;

import com.grow.study.adapter.persistence.StudyJpaRepository;
import com.grow.study.adapter.persistence.dto.CursorResult;
import com.grow.study.adapter.persistence.dto.StudyListResponse;
import com.grow.study.adapter.persistence.dto.StudySearchCondition;
import com.grow.study.adapter.persistence.dto.StudySearchCondition.StudySortType;
import com.grow.study.application.dto.StudyDashboardResponse;
import com.grow.study.application.provided.StudyFinder;
import com.grow.study.application.required.*;
import com.grow.study.application.required.dto.MemberSummaryResponse;
import com.grow.study.application.required.dto.StudySummaryResponse;
import com.grow.study.application.required.dto.StudyWithMemberCountDto;
import com.grow.study.application.required.dto.StudyWithMemberCountResponse;
import com.grow.study.domain.study.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class StudyQueryService implements StudyFinder {

    private final StudyRepository studyRepository;
    private final MemberRestClient memberRestClient;

    @Transactional(readOnly = true)
    @Override
    public StudyWithMemberCountResponse getStudyEnrollmentDetail(Long studyId) {

        Study study = studyRepository.findWithSchedule(studyId).orElseThrow();

        StudyWithMemberCountDto studyWithMemberCountDto = StudyWithMemberCountDto.of(study);


        Set<DayOfWeek> daysOfWeek  = studyWithMemberCountDto.getStudy().getSchedule().getDaysOfWeek();
        Set<String> dayNames = daysOfWeek.stream()
                .map(DayOfWeek::getShortName)
                .collect(Collectors.toSet());

        //member service 연동 필요
        MemberSummaryResponse memberSummary = memberRestClient.getMemberSummary(study.getLeaderId());

        return StudyWithMemberCountResponse.of(studyWithMemberCountDto, memberSummary,dayNames);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<StudyListResponse> getStudyList(
            String level,
            StudyCategory category,
            Integer minDepositAmount,
            Integer maxDepositAmount,
            String sortType,
            Pageable pageable
    ) {
        StudySearchCondition condition = StudySearchCondition.builder()
                .level(parseLevel(level))
                .category(category)
                .minDepositAmount(minDepositAmount)
                .maxDepositAmount(maxDepositAmount)
                .sortType(parseSortType(sortType))
                .build();

        return studyRepository.searchStudyList(condition, pageable);
    }

    @Override
    public CursorResult<StudyListResponse> getStudyListByCursor(String level,
                                                        StudyCategory category,
                                                        Integer minDepositAmount,
                                                        Integer maxDepositAmount,
                                                        String sortType,
                                                        String cursor) {

        StudySearchCondition condition = StudySearchCondition.builder()
                .level(parseLevel(level))
                .category(category)
                .minDepositAmount(minDepositAmount)
                .maxDepositAmount(maxDepositAmount)
                .sortType(parseSortType(sortType))
                .build();

        return  studyRepository.searchStudyListByCursor(condition, cursor, 8);
    }

    @Override
    public StudySummaryResponse getStudySimpleDetail(Long id) {
        Study study = studyRepository.findById(id).orElseThrow();

        return StudySummaryResponse.of(study.getId()
                ,study.getStatus().name()
                ,study.getDepositAmount()
                ,study.getLeaderId());
    }

    /**
     * 진행중인 스터디 대시보드 정보 조회
     */
    @Override
    @Transactional(readOnly = true)
    public StudyDashboardResponse getStudyDashboard(Long studyId, Long memberId) {
        // 스터디 및 관련 정보 조회
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new IllegalArgumentException("스터디를 찾을 수 없습니다."));

        // 스터디가 진행중 상태인지 확인
        if (study.getStatus() != StudyStatus.IN_PROGRESS) {
            throw new IllegalStateException("진행중인 스터디만 대시보드를 조회할 수 있습니다.");
        }

        // 스터디 멤버 정보 조회
        StudyMember studyMember = study.getMembers().stream()
                .filter(member -> member.getMemberId().equals(memberId) && member.isActive())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("스터디 참가자가 아닙니다."));

        // 세션 정보
        List<Session> sessions = study.getSessions();
        int totalSessionCount = sessions.size();

        // 현재 주차 계산
        LocalDate now = LocalDate.now();
        LocalDate startDate = study.getSchedule().getStartDate();
        int currentWeek = (int) ChronoUnit.WEEKS.between(startDate, now) + 1;
        int totalWeeks = (int) study.getSchedule().getTotalWeeks();

        // 출석 정보 계산
        int myAttendanceCount = studyMember.getAttendanceCount();
        int myAbsenceCount = studyMember.getAbsenceCount();

        // 완료된 세션 수 계산
        long completedSessionCount = sessions.stream()
                .filter(session -> session.getStatus() == SessionStatus.COMPLETED)
                .count();

        // 남은 세션 수 = 전체 세션 - 완료된 세션
        int remainingSessionCount = totalSessionCount - (int) completedSessionCount;

        // 다음 스터디 요일
        Set<String> nextStudyDays = study.getSchedule().getDaysOfWeek().stream()
                .map(DayOfWeek::getShortName)
                .collect(Collectors.toSet());

        // 출석률 (StudyMember에서 계산된 값 사용)
        BigDecimal myAttendanceRate = studyMember.getAttendanceRate();

        // 다가오는 세션의 출석 가능 시간 찾기
        LocalDateTime attendanceCheckStartTime = null;
        LocalDateTime attendanceCheckEndTime = null;

        Session nextSession = sessions.stream()
                .filter(session -> session.getStatus() == SessionStatus.SCHEDULED)
                .filter(session -> !session.getSessionDate().isBefore(now))
                .min(Comparator.comparing(Session::getSessionDate))
                .orElse(null);

        if (nextSession != null) {
            attendanceCheckStartTime = nextSession.getAttendanceCheckStartTime();
            attendanceCheckEndTime = nextSession.getAttendanceCheckEndTime();
        }

        return StudyDashboardResponse.builder()
                .title(study.getTitle())
                .category(study.getCategory())
                .level(study.getLevel())
                .currentWeek(currentWeek)
                .totalWeeks(totalWeeks)
                .myAttendanceCount(myAttendanceCount)
                .totalSessionCount(totalSessionCount)
                .myAbsenceCount(myAbsenceCount)
                .remainingSessionCount(remainingSessionCount)
                .nextStudyDays(nextStudyDays)
                .myAttendanceRate(myAttendanceRate)
                .attendanceCheckStartTime(attendanceCheckStartTime)
                .attendanceCheckEndTime(attendanceCheckEndTime)
                .build();
    }





    private StudyLevel parseLevel(String level) {
        if (level == null || level.isBlank()) {
            return null;
        }
        return StudyLevel.fromDisplayName(level);
    }

    private StudySortType parseSortType(String sortType) {
        if (sortType == null || sortType.isBlank()) {
            return StudySortType.LATEST;
        }
        return StudySortType.valueOf(sortType);
    }
}
