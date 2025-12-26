package com.grow.study.domain.event;

import com.grow.study.domain.study.StudyStatus;

import java.time.LocalDate;

public record StudyStatusChangedEvent(
        Long studyId,
        StudyStatus newStatus,
        LocalDate recruitEndDate,
        LocalDate startDate,
        LocalDate endDate
) {
    public static StudyStatusChangedEvent of(Long studyId, StudyStatus newStatus,
                                              LocalDate recruitEndDate, LocalDate startDate, LocalDate endDate) {
        return new StudyStatusChangedEvent(studyId, newStatus, recruitEndDate, startDate, endDate);
    }
}
