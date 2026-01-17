package com.grow.common.event.payload;

import com.grow.common.event.EventPayload;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyViewEventPayload implements EventPayload {

    private Long studyId;
    private Long studyViewCount;
}
