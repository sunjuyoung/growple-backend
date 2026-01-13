package com.grow.study.application.dto;

import com.grow.study.application.required.dto.MemberSummaryResponse;
import com.grow.study.domain.study.StudyMember;
import com.grow.study.domain.study.StudyMemberRole;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StudyMemberDetailResponse(
        Long memberId,
        String nickname,
        String profileImageUrl,
        int level,
        StudyMemberRole role,
        Integer depositPaid,
        Integer attendanceCount,
        Integer absenceCount,
        BigDecimal attendanceRate,
        Integer postCount,
        Integer commentCount,
        LocalDateTime joinedAt
) {
    public static StudyMemberDetailResponse of(StudyMember studyMember, MemberSummaryResponse memberInfo) {
        return new StudyMemberDetailResponse(
                studyMember.getMemberId(),
                memberInfo != null ? memberInfo.nickname() : studyMember.getNickname(),
                memberInfo != null ? memberInfo.profileImageUrl() : null,
                memberInfo != null ? memberInfo.level() : 0,
                studyMember.getRole(),
                studyMember.getDepositPaid(),
                studyMember.getAttendanceCount(),
                studyMember.getAbsenceCount(),
                studyMember.getAttendanceRate(),
                studyMember.getPostCount(),
                studyMember.getCommentCount(),
                studyMember.getJoinedAt()
        );
    }
}
