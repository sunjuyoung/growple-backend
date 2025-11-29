package com.grow.member.domain.member;

import com.grow.member.domain.AbstractEntity;
import com.grow.member.domain.Email;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;


@Entity
@Getter
@ToString(callSuper = true, exclude = {"socialAccounts", "interests", "notificationSetting"})
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@NaturalIdCache //이메일로 검색할 때도 JPA영속 컨텍스트 안에 이메일을 가진게 있는지 체크하고 가져온다
public class Member extends AbstractEntity {

    @Embedded
    @NaturalId
    private Email email;

    @Column(length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role = MemberRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status = MemberStatus.ACTIVE;

    // ==================== 프로필 정보 ====================

    @Column(nullable = false, unique = true, length = 30)
    private String nickname;

    @Column(length = 500)
    private String profileImageUrl;

    @Column(length = 500)
    private String introduction;

    // ==================== 포인트 & 레벨 시스템 ====================

    @Column(nullable = false)
    private Integer point = 10_000; // 가입 시 기본 10,000P 지급

    @Column(nullable = false)
    private Integer activityScore = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberLevel level = MemberLevel.NEWCOMER;

    // ==================== 신뢰도 지표 ====================

    @Column(nullable = false)
    @Comment("완주한 스터디 수")
    private Integer completedStudyCount = 0;

    @Column(nullable = false, precision = 5, scale = 2)
    @Comment("평균 출석률 (%)")
    private BigDecimal averageAttendanceRate = BigDecimal.ZERO;

    @Column(nullable = false)
    @Comment("총 출석 횟수")
    private Integer totalAttendanceCount = 0;

    @Column(nullable = false)
    @Comment("총 스터디 참여 횟수")
    private Integer totalStudySessionCount = 0;

    // ==================== 알림 설정 (Embedded) ====================

    @Embedded
    private NotificationSetting notificationSetting = new NotificationSetting();

    // ==================== 연관 관계 ====================

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "member_interests",
            joinColumns = @JoinColumn(name = "member_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "interest", length = 30)
    @Comment("관심 분야")
    private Set<InterestCategory> interests = new HashSet<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SocialAccount> socialAccounts = new HashSet<>();

    // ==================== 감사(Audit) 정보 ====================

    @Column(nullable = false, updatable = false)
    @Comment("가입일시")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Comment("수정일시")
    private LocalDateTime updatedAt;

    @Column
    @Comment("마지막 로그인 일시")
    private LocalDateTime lastLoginAt;


    // ==================== 생성자 ====================

    @Builder
    public Member(String email, String password, String nickname) {
        this.email = new Email(email);
        this.password = password;
        this.nickname = nickname;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // ==================== 비즈니스 메서드 ====================

    /**
     * 프로필 정보 수정
     */
    public void updateProfile(String nickname, String introduction, String profileImageUrl) {
        this.nickname = nickname;
        this.introduction = introduction;
        this.profileImageUrl = profileImageUrl;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 관심 분야 설정
     */
    public void updateInterests(Set<InterestCategory> interests) {
        this.interests.clear();
        this.interests.addAll(interests);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 포인트 충전
     */
    public void chargePoint(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }
        this.point += amount;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 포인트 사용 (스터디 참가비 결제 등)
     */
    public void usePoint(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("사용 금액은 0보다 커야 합니다.");
        }
        if (this.point < amount) {
            throw new IllegalStateException("포인트가 부족합니다.");
        }
        this.point -= amount;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 포인트 환급
     */
    public void refundPoint(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("환급 금액은 0보다 커야 합니다.");
        }
        this.point += amount;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 활동 점수 추가 및 레벨 갱신
     */
    public void addActivityScore(int score) {
        this.activityScore += score;
        updateLevel();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 레벨 자동 갱신
     */
    private void updateLevel() {
        MemberLevel newLevel = MemberLevel.fromScore(this.activityScore);
        if (this.level != newLevel) {
            MemberLevel oldLevel = this.level;
            this.level = newLevel;
            // 레벨업 시 보너스 포인트 지급 (레벨당 1,000P)
            if (newLevel.ordinal() > oldLevel.ordinal()) {
                this.point += 1_000;
            }
        }
    }

    /**
     * 스터디 완주 처리
     */
    public void completeStudy(BigDecimal attendanceRate) {
        this.completedStudyCount++;
        recalculateAverageAttendanceRate(attendanceRate);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 출석 기록 (평균 출석률 재계산용)
     */
    public void recordAttendance(boolean attended) {
        this.totalStudySessionCount++;
        if (attended) {
            this.totalAttendanceCount++;
        }
        recalculateAverageAttendanceRateFromTotal();
        this.updatedAt = LocalDateTime.now();
    }

    private void recalculateAverageAttendanceRate(BigDecimal newRate) {
        if (this.completedStudyCount == 1) {
            this.averageAttendanceRate = newRate;
        } else {
            // 가중 평균 계산
            BigDecimal total = this.averageAttendanceRate
                    .multiply(BigDecimal.valueOf(this.completedStudyCount - 1))
                    .add(newRate);
            this.averageAttendanceRate = total.divide(
                    BigDecimal.valueOf(this.completedStudyCount),
                    2,
                    java.math.RoundingMode.HALF_UP
            );
        }
    }

    private void recalculateAverageAttendanceRateFromTotal() {
        if (this.totalStudySessionCount > 0) {
            this.averageAttendanceRate = BigDecimal.valueOf(this.totalAttendanceCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(this.totalStudySessionCount), 2, java.math.RoundingMode.HALF_UP);
        }
    }

    /**
     * 소셜 계정 연동
     */
    public void linkSocialAccount(SocialAccount socialAccount) {
        this.socialAccounts.add(socialAccount);
        socialAccount.setMember(this);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 로그인 시간 갱신
     */
    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }

    /**
     * 이메일 인증 완료
     */
//    public void verifyEmail() {
//        this.emailVerified = true;
//        this.updatedAt = LocalDateTime.now();
//    }

    /**
     * 비밀번호 변경
     */
    public void changePassword(String newPassword) {
        this.password = newPassword;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 회원 탈퇴
     */
    public void withdraw() {
        this.status = MemberStatus.WITHDRAWN;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 회원 정지
     */
    public void suspend() {
        this.status = MemberStatus.SUSPENDED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 활동 가능 여부 확인
     */
    public boolean isActive() {
        return this.status == MemberStatus.ACTIVE;
    }

}
