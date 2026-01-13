package com.grow.study.application;

import com.grow.study.adapter.persistence.StudyJpaRepository;
import com.grow.study.adapter.persistence.StudyMemberJpaRepository;
import com.grow.study.adapter.persistence.dto.CursorResult;
import com.grow.study.adapter.persistence.dto.StudyListResponse;
import com.grow.study.adapter.persistence.dto.StudySearchCondition;
import com.grow.study.adapter.persistence.dto.StudySearchCondition.StudySortType;
import com.grow.study.application.dto.MyStudiesResponse;
import com.grow.study.application.dto.MyStudySummary;
import com.grow.study.application.dto.StudyDashboardResponse;
import com.grow.study.application.dto.StudyMemberDetailResponse;
import com.grow.study.application.dto.StudyMemberListResponse;
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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class StudyQueryService implements StudyFinder {

    private final StudyRepository studyRepository;
    private final StudyMemberJpaRepository studyMemberJpaRepository;
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
        Study study = studyRepository.findStudyDashBoard(studyId)
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
        boolean isTodayAttendance = false;


        // 오늘의 출석 체크 대상 세션 찾기
        Long todaySessionId = null;

        Session todaySession = sessions.stream()
                .filter(session -> session.getSessionDate().equals(now))
                .findFirst()
                .orElse(null);

//        Session nextSession = sessions.stream()
//                .filter(session -> session.getStatus() == SessionStatus.SCHEDULED)
//                .filter(session -> !session.getSessionDate().isBefore(now))
//                .min(Comparator.comparing(Session::getSessionDate))
//                .orElse(null);

        if (todaySession != null) {
            attendanceCheckStartTime = todaySession.getAttendanceCheckStartTime();
            attendanceCheckEndTime = todaySession.getAttendanceCheckEndTime();
            todaySessionId = todaySession.getId();

            //오늘 세션이 존재한다면 오늘 세션에 출석 체크 했는지?
            Attendance attendance1 = todaySession.getAttendances().stream()
                    .filter(attendance -> attendance.getMemberId().equals(memberId))
                    .findFirst()
                    .orElse(null);

            if(attendance1 != null){
                isTodayAttendance = true;
            }

        }






        return StudyDashboardResponse.builder()
                .todayAttendance(isTodayAttendance)
                .todaySessionId(todaySessionId)
                .id(studyId)
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

    /**
     * 내 스터디 목록 조회 (참여중, 예정, 완료)
     * 1번 쿼리로 모두 조회 후 애플리케이션에서 분류
     */
    @Override
    @Transactional(readOnly = true)
    public MyStudiesResponse getMyStudies(Long memberId) {
        List<Study> allStudies = studyMemberJpaRepository.findAllMyStudies(memberId);
        LocalDate today = LocalDate.now();

        List<MyStudySummary> participating = new ArrayList<>();
        List<MyStudySummary> upcoming = new ArrayList<>();
        List<MyStudySummary> completed = new ArrayList<>();

        for (Study study : allStudies) {
            String status = classifyMyStudyStatus(study, today);

            switch (status) {
                case "PARTICIPATING" -> participating.add(MyStudySummary.fromWithWeekInfo(study));
                case "UPCOMING" -> upcoming.add(MyStudySummary.from(study));
                case "COMPLETED" -> completed.add(MyStudySummary.from(study));
            }
        }

        return MyStudiesResponse.of(participating, upcoming, completed);
    }

    /**
     * 스터디 상태 분류
     * - PARTICIPATING: 진행 중 (IN_PROGRESS)
     * - UPCOMING: 예정 (모집 중 or 모집 마감, 아직 시작 전)
     * - COMPLETED: 완료 (COMPLETED, SETTLED)
     */
    private String classifyMyStudyStatus(Study study, LocalDate today) {
        StudyStatus status = study.getStatus();

        // 진행 중
        if (status == StudyStatus.IN_PROGRESS) {
            return "PARTICIPATING";
        }

        // 완료 상태
        if (status == StudyStatus.COMPLETED || status == StudyStatus.SETTLED) {
            return "COMPLETED";
        }

        // 모집 중 or 모집 마감 -> 예정
        if (status == StudyStatus.RECRUITING || status == StudyStatus.RECRUIT_CLOSED) {
            return "UPCOMING";
        }

        // 기본값 (예외 상황 대비)
        return "UPCOMING";
    }

    /**
     * 스터디 멤버 리스트 조회
     * N+1 문제 방지를 위해 벌크 조회 사용
     */
    @Override
    @Transactional(readOnly = true)
    public StudyMemberListResponse getStudyMembers(Long studyId) {
        // 1. 스터디 정보 조회
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new IllegalArgumentException("스터디를 찾을 수 없습니다."));

        // 2. 스터디의 활성 멤버 조회
        List<StudyMember> studyMembers = studyMemberJpaRepository.findByStudyId(studyId);

        if (studyMembers.isEmpty()) {
            return StudyMemberListResponse.of(studyId, study.getTitle(), List.of());
        }

        // 3. memberId 리스트 추출
        List<Long> memberIds = studyMembers.stream()
                .map(StudyMember::getMemberId)
                .toList();

        // 4. member-service 벌크 조회 (N+1 방지)
        List<MemberSummaryResponse> memberInfos = memberRestClient.getMemberSummaries(memberIds);

        // 5. memberId -> MemberSummaryResponse 매핑
        Map<Long, MemberSummaryResponse> memberInfoMap = memberInfos.stream()
                .collect(Collectors.toMap(
                        MemberSummaryResponse::id,
                        Function.identity()
                ));

        // 6. 응답 DTO 조합 (리더 먼저, 가입일 순)
        List<StudyMemberDetailResponse> memberResponses = studyMembers.stream()
                .sorted(Comparator
                        .comparing(StudyMember::isLeader).reversed()
                        .thenComparing(StudyMember::getJoinedAt))
                .map(sm -> StudyMemberDetailResponse.of(sm, memberInfoMap.get(sm.getMemberId())))
                .toList();

        return StudyMemberListResponse.of(studyId, study.getTitle(), memberResponses);
    }
}
