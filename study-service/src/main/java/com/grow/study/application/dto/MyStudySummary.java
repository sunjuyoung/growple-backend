package com.grow.study.application.dto;

import com.grow.study.domain.study.Study;
import com.grow.study.domain.study.StudyCategory;
import com.grow.study.domain.study.StudyLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 내 스터디 목록용 간략 정보 DTO
 */
@Getter
@Builder
public class MyStudySummary {

    private Long id;
    private String title;
    private String thumbnailUrl;
    private StudyCategory category;
    private StudyLevel level;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer currentParticipants;
    private Integer maxParticipants;

    public static MyStudySummary from(Study study) {
        return MyStudySummary.builder()
                .id(study.getId())
                .title(study.getTitle())
                .thumbnailUrl(study.getThumbnailUrl())
                .category(study.getCategory())
                .level(study.getLevel())
                .startDate(study.getSchedule().getStartDate())
                .endDate(study.getSchedule().getEndDate())
                .currentParticipants(study.getCurrentParticipants())
                .maxParticipants(study.getMaxParticipants())
                .build();
    }
}
