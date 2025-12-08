package com.grow.study.adapter.persistence.dto;

import com.grow.study.domain.study.StudyCategory;
import com.grow.study.domain.study.StudyLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudySearchCondition {

    private StudyLevel level;
    private StudyCategory category;
    private Integer minDepositAmount;
    private Integer maxDepositAmount;
    private StudySortType sortType;

    public enum StudySortType {
        LATEST,          // 최신순 (createdAt DESC)
        DEADLINE_SOON,    // 마감임박순 (schedule.startDate ASC)
        POPULARITY       // 인기순 (currentParticipants DESC)
    }
}
