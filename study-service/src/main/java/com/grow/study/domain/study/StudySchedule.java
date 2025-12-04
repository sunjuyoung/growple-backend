package com.grow.study.domain.study;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

/**
 * 스터디 일정 정보를 담는 Value Object
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class StudySchedule {

    @Column(nullable = false)
    @Comment("스터디 시작일")
    private LocalDate startDate;

    @Column(nullable = false)
    @Comment("스터디 종료일")
    private LocalDate endDate;

    @Column(nullable = false)
    @Comment("스터디 시작 시간")
    private LocalTime startTime;

    @Column(nullable = false)
    @Comment("스터디 종료 시간")
    private LocalTime endTime;

    @ElementCollection
    @CollectionTable(
            name = "study_schedule_days",
            joinColumns = @JoinColumn(name = "study_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", length = 20)
    @Comment("스터디 진행 요일")
    private Set<DayOfWeek> daysOfWeek = new HashSet<>();

    /**
     * 총 주차 수 계산
     */
    public long getTotalWeeks() {
        return ChronoUnit.WEEKS.between(startDate, endDate) + 1;
    }

    /**
     * 총 일수 계산
     */
    public long getTotalDays() {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    /**
     * 총 예상 세션 수 계산 (주차 * 요일 수)
     */
    public int getExpectedSessionCount() {
        return (int) (getTotalWeeks() * daysOfWeek.size());
    }

    /**
     * 스터디 진행 시간 (분 단위)
     */
    public long getDurationMinutes() {
        return ChronoUnit.MINUTES.between(startTime, endTime);
    }

    /**
     * 현재 날짜가 스터디 기간에 포함되는지 확인
     */
    public boolean isWithinPeriod(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /**
     * 스터디가 시작되었는지 확인
     */
    public boolean hasStarted() {
        return !LocalDate.now().isBefore(startDate);
    }

    /**
     * 스터디가 종료되었는지 확인
     */
    public boolean hasEnded() {
        return LocalDate.now().isAfter(endDate);
    }

    /**
     * 특정 요일이 스터디 진행 요일인지 확인
     */
    public boolean isStudyDay(DayOfWeek dayOfWeek) {
        return daysOfWeek.contains(dayOfWeek);
    }

    /**
     * 요일 문자열 생성 (예: "월, 수, 금")
     */
    public String getDaysOfWeekDisplay() {
        return daysOfWeek.stream()
                .sorted()
                .map(DayOfWeek::getShortName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }
}
