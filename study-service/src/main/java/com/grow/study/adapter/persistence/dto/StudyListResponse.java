package com.grow.study.adapter.persistence.dto;

import com.grow.study.domain.study.DayOfWeek;
import com.grow.study.domain.study.Study;
import com.grow.study.domain.study.StudyCategory;
import com.grow.study.domain.study.StudyLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@Getter
@Builder
public class StudyListResponse {

    private Long id;
    private String title;
    private String thumbnailUrl;
    private StudyCategory category;
    private StudyLevel level;
    private ScheduleInfo schedule;
    private Integer maxParticipants;
    private Integer currentParticipants;
    private Integer depositAmount;
    private Long totalWeeks;

    @Getter
    @Builder
    public static class ScheduleInfo {
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private LocalDate recruitEndDate;
        private Set<DayOfWeek> daysOfWeek;
    }

    public static StudyListResponse from(Study study) {
        return StudyListResponse.builder()
                .id(study.getId())
                .title(study.getTitle())
                .thumbnailUrl(study.getThumbnailUrl())
                .category(study.getCategory())
                .level(study.getLevel())
                .schedule(ScheduleInfo.builder()
                        .startDate(study.getSchedule().getStartDate())
                        .endDate(study.getSchedule().getEndDate())
                        .startTime(study.getSchedule().getStartTime())
                        .endTime(study.getSchedule().getEndTime())
                        .recruitEndDate(study.getSchedule().getRecruitEndDate())
                        .daysOfWeek(study.getSchedule().getDaysOfWeek())
                        .build())
                .maxParticipants(study.getMaxParticipants())
                .currentParticipants(study.getCurrentParticipants())
                .depositAmount(study.getDepositAmount())
                .totalWeeks(study.getSchedule().getTotalWeeks())
                .build();
    }
}
