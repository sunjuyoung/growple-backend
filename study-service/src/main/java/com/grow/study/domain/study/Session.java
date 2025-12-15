package com.grow.study.domain.study;

import com.grow.study.domain.AbstractEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 스터디 세션(회차) 엔티티
 * 각 스터디의 개별 회차를 나타냄
 */
@Entity
@Table(
        name = "sessions",
        indexes = {
                @Index(name = "idx_session_study_id", columnList = "study_id"),
                @Index(name = "idx_session_date", columnList = "sessionDate")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Session extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id", nullable = false)
    @Comment("스터디 ID")
    private Study study;

    @Column(nullable = false)
    @Comment("회차 번호")
    private Integer sessionNumber;

    @Column(nullable = false)
    @Comment("세션 일자")
    private LocalDate sessionDate;

    @Column(nullable = false)
    @Comment("세션 시작 시간")
    private LocalTime startTime;

    @Column(nullable = false)
    @Comment("세션 종료 시간")
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Comment("세션 상태")
    private SessionStatus status = SessionStatus.SCHEDULED;

    // ==================== 출석 체크 설정 ====================

    @Column(nullable = false)
    @Comment("출석 체크 시작 시간 (세션 시작 30분 전)")
    private LocalDateTime attendanceCheckStartTime;

    @Column(nullable = false)
    @Comment("출석 체크 종료 시간 (세션 시작 10분 후)")
    private LocalDateTime attendanceCheckEndTime;

    // ==================== 세션 내용 ====================

    @Column(length = 100)
    @Comment("세션 제목")
    private String title;

    @Column(length = 1000)
    @Comment("세션 설명")
    private String description;

    @Column(length = 500)
    @Comment("학습 자료 URL")
    private String materialUrl;

    // ==================== 통계 정보 ====================

    @Column(nullable = false)
    @Comment("출석 인원")
    private Integer attendanceCount = 0;

    @Column(nullable = false)
    @Comment("결석 인원")
    private Integer absenceCount = 0;

    // ==================== 연관 관계 ====================

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attendance> attendances = new ArrayList<>();

    // ==================== 감사(Audit) 정보 ====================

    @Column(nullable = false, updatable = false)
    @Comment("생성일시")
    private LocalDateTime createdAt;

    @Column
    @Comment("시작일시")
    private LocalDateTime startedAt;

    @Column
    @Comment("종료일시")
    private LocalDateTime completedAt;

    @Comment("출석 마감 처리 완료 여부")
    @Column(nullable = false)
    private Boolean attendanceProcessed = false;

    // ==================== 생성자 ====================

    @Builder
    public Session(
            Study study,
            Integer sessionNumber,
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime,
            String title,
            String description
    ) {
        this.study = study;
        this.sessionNumber = sessionNumber;
        this.sessionDate = sessionDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.title = title;
        this.description = description;
        this.createdAt = LocalDateTime.now();

        // 출석 체크 시간 자동 설정
        LocalDateTime sessionStartDateTime = LocalDateTime.of(sessionDate, startTime);
        this.attendanceCheckStartTime = sessionStartDateTime.minusMinutes(30);
        this.attendanceCheckEndTime = sessionStartDateTime.plusMinutes(10);
    }

    /**
     * 스터디 일정으로부터 세션 자동 생성
     */
    public static Session createFromSchedule(
            Study study,
            Integer sessionNumber,
            LocalDate sessionDate
    ) {
        StudySchedule schedule = study.getSchedule();

        return Session.builder()
                .study(study)
                .sessionNumber(sessionNumber)
                .sessionDate(sessionDate)
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .title(sessionNumber + "회차")
                .description("")
                .build();
    }

    // ==================== 비즈니스 메서드 ====================

    /**
     * 세션 정보 수정
     */
    public void updateInfo(String title, String description, String materialUrl) {
        if (this.status != SessionStatus.SCHEDULED) {
            throw new IllegalStateException("예정된 세션만 수정할 수 있습니다.");
        }

        this.title = title;
        this.description = description;
        this.materialUrl = materialUrl;
    }

    /**
     * 세션 시작
     */
    public void start() {
        if (this.status != SessionStatus.SCHEDULED) {
            throw new IllegalStateException("예정된 세션만 시작할 수 있습니다.");
        }

        this.status = SessionStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }

    /**
     * 세션 완료
     */
    public void complete() {
        if (this.status != SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("진행 중인 세션만 완료할 수 있습니다.");
        }

        this.status = SessionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 세션 취소
     */
    public void cancel() {
        if (this.status == SessionStatus.COMPLETED) {
            throw new IllegalStateException("완료된 세션은 취소할 수 없습니다.");
        }

        this.status = SessionStatus.CANCELLED;
    }

    /**
     * 출석 체크 가능 시간인지 확인
     */
    public boolean isAttendanceCheckAvailable() {
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(attendanceCheckStartTime) && !now.isAfter(attendanceCheckEndTime);
    }

    public boolean isAttendCheckDay(){
        LocalDate now = LocalDate.now();
        return now.equals(this.sessionDate);
    }

    /**
     * 출석 체크 시작 전인지 확인
     */
    public boolean isBeforeAttendanceCheck() {
        return LocalDateTime.now().isBefore(attendanceCheckStartTime);
    }

    /**
     * 출석 체크 종료 후인지 확인
     */
    public boolean isAfterAttendanceCheck() {
        return LocalDateTime.now().isAfter(attendanceCheckEndTime);
    }

    /**
     * 출석 인원 증가
     */
    public void incrementAttendance() {
        this.attendanceCount++;
    }

    /**
     * 결석 인원 증가
     */
    public void incrementAbsence() {
        this.absenceCount++;
    }

    /**
     * 출석률 계산
     */
    public double getAttendanceRate() {
        int total = this.attendanceCount + this.absenceCount;
        if (total == 0) {
            return 0.0;
        }
        return (double) this.attendanceCount / total * 100;
    }

    /**
     * 세션 진행 시간 (분)
     */
    public long getDurationMinutes() {
        return java.time.Duration.between(startTime, endTime).toMinutes();
    }

    /**
     * 출석 마감 처리 완료
     */
    public void markAttendanceProcessed() {
        this.attendanceProcessed = true;
    }

    /**
     * 출석 마감 처리 여부 확인
     */
    public boolean isAttendanceProcessed() {
        return this.attendanceProcessed;
    }
}
