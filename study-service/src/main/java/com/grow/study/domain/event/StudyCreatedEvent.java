package com.grow.study.domain.event;

import com.grow.study.domain.study.StudyCategory;
import com.grow.study.domain.study.StudyLevel;
import com.grow.study.domain.study.StudyStatus;

import java.time.LocalDate;

/**
 * 스터디 생성 이벤트
 * pgvector Document 생성을 위해 발행
 */
public record StudyCreatedEvent(
        Long studyId,
        String title,
        String introduction,
        String curriculum,
        String leaderMessage,
        StudyCategory category,
        StudyLevel level,
        StudyStatus status,
        Integer minParticipants,
        Integer maxParticipants,
        LocalDate startDate,
        LocalDate endDate
) {
}
