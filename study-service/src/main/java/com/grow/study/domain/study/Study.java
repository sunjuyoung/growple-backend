package com.grow.study.domain.study;

import com.grow.study.domain.AbstractEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Builder
@Entity
@Table(name = "studies", indexes = {
        @Index(name = "idx_study_created_at_id", columnList = "createdAt DESC, id DESC"),
        @Index(name = "idx_study_start_date_id", columnList = "startDate ASC, id ASC"),
        @Index(name = "idx_study_category", columnList = "category"),
        @Index(name = "idx_study_level", columnList = "level"),
        @Index(name = "idx_study_deposit_amount", columnList = "depositAmount")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Study extends AbstractEntity {

    @Column(nullable = false, length = 50)
    @Comment("스터디 제목")
    private String title;

    @Column(length = 500)
    @Comment("썸네일 이미지 URL")
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Comment("카테고리")
    private StudyCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Comment("난이도")
    private StudyLevel level;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Comment("공개 설정")
    private StudyVisibility visibility;

    @Column(nullable = false)
    @Comment("스터디장 회원 ID")
    private Long leaderId;

    // ==================== 일정 정보 ====================

    @Embedded
    private StudySchedule schedule;

    // ==================== 참가 설정 ====================


    @Column(nullable = false)
    @Comment("최소 인원")
    private Integer minParticipants;

    @Column(nullable = false)
    @Comment("최대 인원")
    private Integer maxParticipants;

    @Column(nullable = false)
    @Comment("현재 참가 인원")
    private Integer currentParticipants = 0;

    @Column(nullable = false)
    @Comment("보증금(참가비) 포인트")
    private Integer depositAmount;

    // ==================== 상세 설명 ====================

    @Column(length = 2000)
    @Comment("스터디 소개")
    private String introduction;

    @Column(length = 2000)
    @Comment("진행 방식")
    private String curriculum;

    @Column(length = 500)
    @Comment("스터디장 한마디")
    private String leaderMessage;

    // ==================== 상태 정보 ====================

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StudyStatus status = StudyStatus.PENDING;

    // ==================== 연관 관계 ====================

    @Builder.Default
    @OneToMany(mappedBy = "study", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudyMember> members = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "study", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Session> sessions = new ArrayList<>();

    // ==================== 감사(Audit) 정보 ====================


    @Column(nullable = false, updatable = false)
    @Comment("개설일시")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Comment("수정일시")
    private LocalDateTime updatedAt;

    @Column
    @Comment("시작일시 (스터디 진행 시작)")
    private LocalDateTime startedAt;

    @Column
    @Comment("종료일시 (스터디 완료)")
    private LocalDateTime completedAt;


    // ==================== 생성자 ====================


    public static Study create(
            String title,
            String thumbnailUrl,
            StudyCategory category,
            StudyLevel level,
            StudyVisibility visibility,
            Long leaderId,
            StudySchedule schedule,
            Integer minParticipants,
            Integer maxParticipants,
            Integer depositAmount,
            String introduction,
            String curriculum,
            String leaderMessage,
            StudyStatus status
    ) {
        LocalDateTime now = LocalDateTime.now();

        Study study = new Study();
        study.title = title;
        study.thumbnailUrl = thumbnailUrl;
        study.category = category;
        study.level = level;
        study.visibility = visibility;
        study.leaderId = leaderId;
        study.schedule = schedule;
        study.minParticipants = minParticipants;
        study.maxParticipants = maxParticipants;
        study.depositAmount = depositAmount;
        study.introduction = introduction;
        study.curriculum = curriculum;
        study.leaderMessage = leaderMessage;
        study.status = status;
        study.createdAt = now;
        study.updatedAt = now;
        study.currentParticipants = 1;

        study.validateStudy();
        return study;
    }

    // ==================== 비즈니스 메서드 ====================

    /**
     * 스터디 유효성 검증
     */
    private void validateStudy() {
        if (minParticipants < 1) {
            throw new IllegalArgumentException("최소 인원은 1명 이상이어야 합니다.");
        }
        if (maxParticipants < minParticipants) {
            throw new IllegalArgumentException("최대 인원은 최소 인원보다 크거나 같아야 합니다.");
        }
        if (depositAmount < 5_000 || depositAmount > 50_000) {
            throw new IllegalArgumentException("보증금은 5,000P ~ 50,000P 사이여야 합니다.");
        }
        if( schedule.getEndDate().isBefore(schedule.getStartDate())) {
            throw new IllegalArgumentException("종료일은 시작일 이후여야 합니다.");
        }


    }

    /**
     * 스터디 정보 수정
     */
    public void updateInfo(
            String title,
            String thumbnailUrl,
            String introduction,
            String curriculum,
            String leaderMessage
    ) {
        if (this.status != StudyStatus.RECRUITING) {
            throw new IllegalStateException("모집 중인 스터디만 수정할 수 있습니다.");
        }

        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.introduction = introduction;
        this.curriculum = curriculum;
        this.leaderMessage = leaderMessage;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 참가자 추가
     */
    public void addParticipant() {
        if (this.status != StudyStatus.RECRUITING) {
            throw new IllegalStateException("모집 중인 스터디만 참가할 수 있습니다.");
        }
        if (isFull()) {
            throw new IllegalStateException("정원이 가득 찼습니다.");
        }

        this.currentParticipants++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 참가자 제거
     */
    public void removeParticipant() {
        if (this.currentParticipants <= 0) {
            throw new IllegalStateException("참가자가 없습니다.");
        }

        this.currentParticipants--;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 스터디 시작
     */
    public void start() {
        if (this.status != StudyStatus.RECRUITING) {
            throw new IllegalStateException("모집 중인 스터디만 시작할 수 있습니다.");
        }
        if (this.currentParticipants < this.minParticipants) {
            throw new IllegalStateException("최소 인원이 미달되었습니다.");
        }

        this.status = StudyStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 스터디 완료
     */
    public void complete() {
        if (this.status != StudyStatus.IN_PROGRESS) {
            throw new IllegalStateException("진행 중인 스터디만 완료할 수 있습니다.");
        }

        this.status = StudyStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 스터디 취소 (최소 인원 미달 등)
     */
    public void cancel() {
        if (this.status != StudyStatus.RECRUITING) {
            throw new IllegalStateException("모집 중인 스터디만 취소할 수 있습니다.");
        }

        this.status = StudyStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 스터디 조기 종료
     */
    public void terminateEarly() {
        if (this.status != StudyStatus.IN_PROGRESS) {
            throw new IllegalStateException("진행 중인 스터디만 조기 종료할 수 있습니다.");
        }

        this.status = StudyStatus.EARLY_TERMINATED;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 정원 확인
     */
    public boolean isFull() {
        return this.currentParticipants >= this.maxParticipants;
    }

    /**
     * 모집 마감 임박 여부 (정원의 80% 이상)
     */
    public boolean isAlmostFull() {
        return this.currentParticipants >= this.maxParticipants * 0.8;
    }

    /**
     * 최소 인원 달성 여부
     */
    public boolean hasMinimumParticipants() {
        return this.currentParticipants >= this.minParticipants;
    }

    /**
     * 스터디 시작 가능 여부
     */
    public boolean canStart() {
        return this.status == StudyStatus.RECRUITING
                && hasMinimumParticipants()
                && this.schedule.hasStarted();
    }

    /**
     * 참가 가능 여부
     */
    public boolean isJoinable() {
        return this.status == StudyStatus.RECRUITING
                && !isFull()
                && !this.schedule.hasStarted();
    }

    //스터디 모집 시작
    public void openRecruitment() {
        if (this.status != StudyStatus.PENDING) {
            throw new IllegalStateException("대기 중인 스터디만 모집을 시작할 수 있습니다.");
        }

        this.status = StudyStatus.RECRUITING;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 스터디장 여부 확인
     */
    public boolean isLeader(Long memberId) {
        return this.leaderId.equals(memberId);
    }

    /**
     * 스터디 세션 생성
     * 스터디 일정과 총 세션 수에 따라 세션들을 자동 생성합니다.
     */
    public void createSessions(Integer totalSessions) {
        if (totalSessions == null || totalSessions <= 0) {
            return;
        }

        LocalDate currentDate = this.schedule.getStartDate();
        LocalDate endDate = this.schedule.getEndDate();
        int sessionNumber = 1;

        while (sessionNumber <= totalSessions && !currentDate.isAfter(endDate)) {
            // 현재 날짜의 요일 확인
            DayOfWeek dayOfWeek = DayOfWeek.from(currentDate.getDayOfWeek());

            // 스터디 진행 요일인 경우 세션 생성
            if (this.schedule.isStudyDay(dayOfWeek)) {
                Session session = Session.createFromSchedule(this, sessionNumber, currentDate);
                this.sessions.add(session);
                sessionNumber++;
            }

            // 다음 날로 이동
            currentDate = currentDate.plusDays(1);
        }
    }

    /**
     * 스터디장을 멤버로 추가
     */
    public void addLeaderAsMember() {
        StudyMember leader = StudyMember.createLeader(this, this.leaderId);
        this.members.add(leader);
        this.currentParticipants = 1;
    }

    /**
     * 멤버 추가 (스터디장 제외)
     */
    public void addMember(Long memberId, Integer depositAmount) {
        // 이미 참가한 멤버인지 확인
        boolean alreadyMember = this.members.stream()
                .anyMatch(m -> m.getMemberId().equals(memberId) && m.isActive());

        if (alreadyMember) {
            throw new IllegalStateException("이미 참가한 멤버입니다.");
        }

        StudyMember member = StudyMember.createMember(this, memberId, depositAmount);
        this.members.add(member);
        addParticipant();
    }
}
