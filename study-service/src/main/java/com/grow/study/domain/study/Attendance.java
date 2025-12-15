package com.grow.study.domain.study;

import com.grow.study.domain.AbstractEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 출석 엔티티
 * 각 세션에 대한 참가자의 출석 정보를 기록
 */
@Entity
@Table(
        name = "attendances",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"session_id", "member_id"})
        },
        indexes = {
                @Index(name = "idx_attendance_session_id", columnList = "session_id"),
                @Index(name = "idx_attendance_member_id", columnList = "member_id"),
                @Index(name = "idx_attendance_status", columnList = "status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attendance extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @Comment("세션 ID")
    private Session session;

    @Column(name = "member_id", nullable = false)
    @Comment("회원 ID")
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Comment("출석 상태")
    private AttendanceStatus status;

    @Column
    @Comment("출석 체크 시간")
    private LocalDateTime checkedAt;

    @Column(length = 500)
    @Comment("비고 (지각/결석 사유 등)")
    private String note;

    // ==================== 활동 점수 ====================

    @Column(nullable = false)
    @Comment("획득한 활동 점수")
    private Integer activityScore = 0;

    // ==================== 감사(Audit) 정보 ====================

    @Column(nullable = false, updatable = false)
    @Comment("생성일시")
    private LocalDateTime createdAt;

    @Column
    @Comment("수정일시")
    private LocalDateTime updatedAt;

    // ==================== 생성자 ====================

    @Builder
    public Attendance(
            Session session,
            Long memberId,
            AttendanceStatus status,
            LocalDateTime checkedAt,
            String note
    ) {
        this.session = session;
        this.memberId = memberId;
        this.status = status;
        this.checkedAt = checkedAt;
        this.note = note;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        // 출석 시 활동 점수 부여
        if (status == AttendanceStatus.PRESENT) {
            this.activityScore = 5;
        }
    }

    /**
     * 출석 체크 생성
     */
    public static Attendance createPresent(Session session, Long memberId) {

        return Attendance.builder()
                .session(session)
                .memberId(memberId)
                .status(AttendanceStatus.PRESENT)
                .checkedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 결석 처리 (자동)
     */
    public static Attendance createAbsent(Session session, Long memberId) {
        return Attendance.builder()
                .session(session)
                .memberId(memberId)
                .status(AttendanceStatus.ABSENT)
                .note("출석 체크 시간 내 미체크")
                .build();
    }

    // ==================== 비즈니스 메서드 ====================

    /**
     * 출석 상태 변경
     */
    public void updateStatus(AttendanceStatus newStatus, String note) {
        // 이미 처리된 출석은 변경 제한 (스터디장 권한 필요 등)
        if (this.status == AttendanceStatus.PRESENT && newStatus == AttendanceStatus.ABSENT) {
            throw new IllegalStateException("출석을 결석으로 변경할 수 없습니다.");
        }

        AttendanceStatus oldStatus = this.status;
        this.status = newStatus;
        this.note = note;
        this.updatedAt = LocalDateTime.now();

        // 활동 점수 조정
        adjustActivityScore(oldStatus, newStatus);
    }

    /**
     * 활동 점수 조정
     */
    private void adjustActivityScore(AttendanceStatus oldStatus, AttendanceStatus newStatus) {
        // 기존 점수 제거
        if (oldStatus == AttendanceStatus.PRESENT) {
            this.activityScore = 0;
        }

        // 새 점수 부여
        if (newStatus == AttendanceStatus.PRESENT) {
            this.activityScore = 5;
        }
    }


    //미처리 기능
//    /**
//     * 사유 결석으로 변경 (스터디장 권한)
//     */
//    public void excuseAbsence(String reason) {
//        if (this.status != AttendanceStatus.ABSENT) {
//            throw new IllegalStateException("결석 상태만 사유 결석으로 변경할 수 있습니다.");
//        }
//
//        this.status = AttendanceStatus.EXCUSED;
//        this.note = reason;
//        this.updatedAt = LocalDateTime.now();
//    }
//
//    /**
//     * 지각 처리 (선택적 기능)
//     */
//    public void markAsLate(String reason) {
//        if (this.status == AttendanceStatus.PRESENT) {
//            this.status = AttendanceStatus.LATE;
//            this.note = reason;
//            this.updatedAt = LocalDateTime.now();
//        }
//    }

    /**
     * 출석 여부 확인
     */
    public boolean isPresent() {
        return this.status.isPresent();
    }

    /**
     * 결석 여부 확인
     */
    public boolean isAbsent() {
        return this.status.isAbsent();
    }
}
