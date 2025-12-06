package com.grow.study.application.required.dto;

import com.grow.study.domain.study.StudyCategory;
import com.grow.study.domain.study.StudyLevel;
import com.grow.study.domain.study.StudyStatus;

import java.time.LocalDate;

public record StudyWithMemberCountResponse(
        Long id,
        String title,
        String thumbnailUrl,
        String category,
        String staudyLevel,
        Long leaderId,
        LocalDate startDate,
        LocalDate endDate,
        Integer minParticipants,
        Integer maxParticipants,
        Integer currentParticipants,
        Integer depositAmount,
        StudyStatus status,
        Long memberCount,
        Long userId,
        String nickname,
        String profileImageUrl,
        int level
) {
    public static StudyWithMemberCountResponse of(StudyWithMemberCountDto dto, MemberSummaryResponse memberSummaryResponse){
        return new StudyWithMemberCountResponse(
                dto.study.getId(),
                dto.study.getTitle(),
                dto.study.getThumbnailUrl(),
                dto.study.getCategory().getDisplayName(),
                dto.study.getLevel().getDisplayName(),
                dto.study.getLeaderId(),
                dto.study.getSchedule().getStartDate(),
                dto.study.getSchedule().getEndDate(),
                dto.study.getMinParticipants(),
                dto.study.getMaxParticipants(),
                dto.study.getCurrentParticipants(),
                dto.study.getDepositAmount(),
                dto.study.getStatus(),
                dto.memberCount,

                memberSummaryResponse.id(),
                memberSummaryResponse.nickname(),
                memberSummaryResponse.profileImageUrl(),
                memberSummaryResponse.level()
        );

    }
}