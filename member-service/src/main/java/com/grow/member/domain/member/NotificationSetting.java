package com.grow.member.domain.member;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSetting {

    @Column(nullable = false)
    @Comment("출석 알림 (스터디 시작 30분 전)")
    private boolean attendanceNotification = true;

    @Column(nullable = false)
    @Comment("환급 알림")
    private boolean refundNotification = true;

    @Column(nullable = false)
    @Comment("레벨업 알림")
    private boolean levelUpNotification = true;

    @Column(nullable = false)
    @Comment("참가 승인 알림")
    private boolean participationNotification = true;

    @Column(nullable = false)
    @Comment("스터디 시작 알림")
    private boolean studyStartNotification = true;

    /**
     * 알림 설정 변경
     */
    public void update(
            boolean attendanceNotification,
            boolean refundNotification,
            boolean levelUpNotification,
            boolean participationNotification,
            boolean studyStartNotification
    ) {
        this.attendanceNotification = attendanceNotification;
        this.refundNotification = refundNotification;
        this.levelUpNotification = levelUpNotification;
        this.participationNotification = participationNotification;
        this.studyStartNotification = studyStartNotification;
    }

}