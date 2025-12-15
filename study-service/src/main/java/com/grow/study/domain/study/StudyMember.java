package com.grow.study.domain.study;

import com.grow.study.domain.AbstractEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 스터디 참가자 엔티티
 * 스터디와 회원 간의 관계를 나타냄
 */
@Entity
@Table(
        name = "study_members",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"study_id", "member_id"})
        },
        indexes = {
                @Index(name = "idx_study_member_study_id", columnList = "study_id"),
                @Index(name = "idx_study_member_member_id", columnList = "member_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyMember extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id", nullable = false)
    @Comment("스터디 ID")
    private Study study;

    @Column(name = "member_id", nullable = false)
    @Comment("회원 ID")
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Comment("역할")
    private StudyMemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Comment("상태")
    private StudyMemberStatus status = StudyMemberStatus.ACTIVE;

    // ==================== 결제 정보 ====================

    @Column(nullable = false)
    @Comment("지불한 보증금")
    private Integer depositPaid;

    @Column
    @Comment("환급받은 금액")
    private Integer refundAmount = 0;

    @Column
    @Comment("환급 일시")
    private LocalDateTime refundedAt;


    @Column
    @Comment("차감된 금액")
    private Integer deductedDeposit;

    // ==================== 출석 정보 ====================

    @Column(nullable = false)
    @Comment("출석 횟수")
    private Integer attendanceCount = 0;

    @Column(nullable = false)
    @Comment("결석 횟수")
    private Integer absenceCount = 0;

    @Column(nullable = false, precision = 5, scale = 2)
    @Comment("출석률 (%)")
    private BigDecimal attendanceRate = BigDecimal.ZERO;

    // ==================== 활동 정보 ====================

    @Column(nullable = false)
    @Comment("작성한 게시글 수")
    private Integer postCount = 0;

    @Column(nullable = false)
    @Comment("작성한 댓글 수")
    private Integer commentCount = 0;

    // ==================== 감사(Audit) 정보 ====================

    @Column(nullable = false, updatable = false)
    @Comment("참가일시")
    private LocalDateTime joinedAt;

    @Column
    @Comment("탈퇴일시")
    private LocalDateTime withdrawnAt;

    // ==================== 생성자 ====================

    @Builder
    public StudyMember(
            Study study,
            Long memberId,
            StudyMemberRole role,
            Integer depositPaid
    ) {
        this.study = study;
        this.memberId = memberId;
        this.role = role;
        this.depositPaid = depositPaid;
        this.joinedAt = LocalDateTime.now();
    }

    /**
     * 스터디장 생성
     */
    public static StudyMember createLeader(Study study, Long leaderId) {
        return StudyMember.builder()
                .study(study)
                .memberId(leaderId)
                .role(StudyMemberRole.LEADER)
                .depositPaid(0) // 스터디장은 보증금 없음
                .build();
    }

    /**
     * 일반 참가자 생성
     */
    public static StudyMember createMember(Study study, Long memberId, Integer depositPaid) {
        return StudyMember.builder()
                .study(study)
                .memberId(memberId)
                .role(StudyMemberRole.MEMBER)
                .depositPaid(depositPaid)
                .build();
    }

    // ==================== 비즈니스 메서드 ====================

    /**
     * 출석 기록
     */
    public void recordAttendance(boolean attended) {
        if (attended) {
            this.attendanceCount++;
        } else {
            this.absenceCount++;
        }
        recalculateAttendanceRate();
    }

    /**
     * 출석률 재계산
     */
    private void recalculateAttendanceRate() {
        int totalSessions = this.attendanceCount + this.absenceCount;
        if (totalSessions > 0) {
            this.attendanceRate = BigDecimal.valueOf(this.attendanceCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalSessions), 2, java.math.RoundingMode.HALF_UP);
        }
    }

    /**
     * 환급 처리
     */
    public void refund(Integer amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("환급 금액은 0보다 커야 합니다.");
        }
        if (this.refundAmount > 0) {
            throw new IllegalStateException("이미 환급 처리되었습니다.");
        }

        this.refundAmount = amount;
        this.refundedAt = LocalDateTime.now();
    }


    public void deductDeposit() {
        this.deductedDeposit += 1000;
    }

    /**
     * 환급 금액 계산
     */
    public Integer calculateRefundAmount() {
        if (this.role == StudyMemberRole.LEADER) {
            return 0; // 스터디장은 환급 대상 아님
        }

        BigDecimal rate = this.attendanceRate;

        // 100% 출석: 100% + 보너스 5%
        if (rate.compareTo(BigDecimal.valueOf(100)) == 0) {
            return (int) (this.depositPaid * 1.05);
        }
        // 80% 이상: 80% 환급
        else if (rate.compareTo(BigDecimal.valueOf(80)) >= 0) {
            return (int) (this.depositPaid * 0.8);
        }
        // 60% 이상: 60% 환급
        else if (rate.compareTo(BigDecimal.valueOf(60)) >= 0) {
            return (int) (this.depositPaid * 0.6);
        }
        // 60% 미만: 환급 없음
        else {
            return 0;
        }
    }

    /**
     * 탈퇴 처리
     */
    public void withdraw() {
        if (this.status != StudyMemberStatus.ACTIVE) {
            throw new IllegalStateException("활동 중인 멤버만 탈퇴할 수 있습니다.");
        }
        if (this.role == StudyMemberRole.LEADER) {
            throw new IllegalStateException("스터디장은 탈퇴할 수 없습니다.");
        }

        this.status = StudyMemberStatus.WITHDRAWN;
        this.withdrawnAt = LocalDateTime.now();
    }

    /**
     * 강제 퇴장 처리
     */
    public void expel() {
        if (this.status != StudyMemberStatus.ACTIVE) {
            throw new IllegalStateException("활동 중인 멤버만 퇴장시킬 수 있습니다.");
        }
        if (this.role == StudyMemberRole.LEADER) {
            throw new IllegalStateException("스터디장은 퇴장시킬 수 없습니다.");
        }

        this.status = StudyMemberStatus.EXPELLED;
        this.withdrawnAt = LocalDateTime.now();
    }

    /**
     * 게시글 작성 카운트 증가
     */
    public void incrementPostCount() {
        this.postCount++;
    }

    /**
     * 댓글 작성 카운트 증가
     */
    public void incrementCommentCount() {
        this.commentCount++;
    }

    /**
     * 스터디장 여부
     */
    public boolean isLeader() {
        return this.role == StudyMemberRole.LEADER;
    }

    /**
     * 활동 중인지 확인
     */
    public boolean isActive() {
        return this.status.isActive();
    }
}
