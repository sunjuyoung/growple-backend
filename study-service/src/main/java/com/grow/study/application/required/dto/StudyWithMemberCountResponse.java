package com.grow.study.application.required.dto;


import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public record StudyWithMemberCountResponse(
        Long id,
        String title,
        String thumbnailUrl,
        String category,
        String studyLevel,
        String introduction,
        String curriculum,
        String leaderMessage,
        Long leaderId,
        LocalDate startDate,
        LocalDate endDate,
        LocalTime startTime,
        LocalTime endTime,
        Integer minParticipants,
        Integer maxParticipants,
        Integer currentParticipants,
        Integer depositAmount,
        String studyStatus,
        Integer memberCount,
        Long userId,
        String nickname,
        String profileImageUrl,
        int memberLevel,
        Set<String> daysOfWeek,
        //스터디 총일수
        Long totalDays,
        //스터디 총주차
        Long totalWeeks,
        // 세션 수
        int totalSessions,
        // 세션 당 시간(분)
        Long duringMinutes,
        //모집 마감일
        LocalDate recruitEndDate
) {
    public static StudyWithMemberCountResponse of(StudyWithMemberCountDto dto, MemberSummaryResponse memberSummaryResponse, Set<String> dayOfWeeks ){
        return new StudyWithMemberCountResponse(
                dto.study.getId(),
                dto.study.getTitle(),
                dto.study.getThumbnailUrl(),
                dto.study.getCategory().getDisplayName(),
                dto.study.getLevel().getDisplayName(),
                dto.study.getIntroduction(),
                dto.study.getCurriculum(),
                dto.study.getLeaderMessage(),
                dto.study.getLeaderId(),
                dto.study.getSchedule().getStartDate(),
                dto.study.getSchedule().getEndDate(),
                dto.study.getSchedule().getStartTime(),
                dto.study.getSchedule().getEndTime(),
                dto.study.getMinParticipants(),
                dto.study.getMaxParticipants(),
                dto.study.getCurrentParticipants(),
                dto.study.getDepositAmount(),
                dto.study.getStatus().getDisplayName(),
                dto.study.getCurrentParticipants(),
                memberSummaryResponse.id(),
                memberSummaryResponse.nickname(),
                memberSummaryResponse.profileImageUrl(),
                memberSummaryResponse.level(),
                dayOfWeeks,
                dto.study.getSchedule().getTotalDays(),
                dto.study.getSchedule().getTotalWeeks(),
                dto.study.getSchedule().getExpectedSessionCount(),
                dto.study.getSchedule().getDurationMinutes(),
                dto.study.getSchedule().getRecruitEndDate()
        );

    }
}