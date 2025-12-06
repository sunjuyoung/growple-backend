package com.grow.study.application.provided.dto;

import com.grow.study.domain.study.Study;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyRegisterResponse {

    private Long studyId;
    private String title;
    private String thumbnailUrl;
    private String category;
    private String level;
    private String visibility;
    private Integer minParticipants;
    private Integer maxParticipants;
    private Integer depositAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;

    public static StudyRegisterResponse from(Study study) {
        return StudyRegisterResponse.builder()
                .studyId(study.getId())
                .title(study.getTitle())
                .thumbnailUrl(study.getThumbnailUrl())
                .category(study.getCategory().getDisplayName())
                .level(study.getLevel().getDisplayName())
                .visibility(study.getVisibility().getDisplayName())
                .minParticipants(study.getMinParticipants())
                .maxParticipants(study.getMaxParticipants())
                .depositAmount(study.getDepositAmount())
                .startDate(study.getSchedule().getStartDate())
                .endDate(study.getSchedule().getEndDate())
                .startTime(study.getSchedule().getStartTime())
                .endTime(study.getSchedule().getEndTime())
                .status(study.getStatus().name())
                .build();
    }
}
