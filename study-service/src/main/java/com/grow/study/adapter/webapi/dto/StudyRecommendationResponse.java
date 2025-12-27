package com.grow.study.adapter.webapi.dto;

import com.grow.study.domain.study.Study;
import com.grow.study.domain.study.StudyCategory;
import com.grow.study.domain.study.StudyLevel;
import com.grow.study.domain.study.StudyStatus;
import lombok.Builder;

import java.time.LocalDate;

/**
 * 스터디 추천 응답 DTO
 */
@Builder
public record StudyRecommendationResponse(
        Long studyId,
        String title,
        String thumbnailUrl,
        StudyCategory category,
        StudyLevel level,
        StudyStatus status,
        Integer minParticipants,
        Integer maxParticipants,
        Integer currentParticipants,
        Integer depositAmount,
        LocalDate startDate,
        LocalDate endDate,
        String introduction
) {
    public static StudyRecommendationResponse from(Study study) {
        return StudyRecommendationResponse.builder()
                .studyId(study.getId())
                .title(study.getTitle())
                .thumbnailUrl(study.getThumbnailUrl())
                .category(study.getCategory())
                .level(study.getLevel())
                .status(study.getStatus())
                .minParticipants(study.getMinParticipants())
                .maxParticipants(study.getMaxParticipants())
                .currentParticipants(study.getCurrentParticipants())
                .depositAmount(study.getDepositAmount())
                .startDate(study.getSchedule().getStartDate())
                .endDate(study.getSchedule().getEndDate())
                .introduction(study.getIntroduction())
                .build();
    }
}
