package com.grow.study.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grow.study.domain.study.StudyCategory;
import com.grow.study.domain.study.StudyLevel;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Builder
public class StudyDashboardResponse {

    private Long id;
    private Long todaySessionId;
    private boolean todayAttendance;
    // 스터디 기본 정보
    private String title;
    private StudyCategory category;
    private StudyLevel level;

    // 진행 정보
    private Integer currentWeek;
    private Integer totalWeeks;

    // 출석 정보
    private Integer myAttendanceCount;
    private Integer totalSessionCount;
    private Integer myAbsenceCount;
    private Integer remainingSessionCount;

    // 다음 스터디 정보
    private Set<String> nextStudyDays;

    // 출석률
    private BigDecimal myAttendanceRate;

    // 출석 가능 시간
    private LocalDateTime attendanceCheckStartTime;
    private LocalDateTime attendanceCheckEndTime;
}
