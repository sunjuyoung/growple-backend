package com.grow.favorite.domain.view;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudyViewCountResponse {

    private Long studyId;
    private Long count;

    public static StudyViewCountResponse of(Long studyId, Long count) {
        return StudyViewCountResponse.builder()
                .studyId(studyId)
                .count(count)
                .build();
    }
}
