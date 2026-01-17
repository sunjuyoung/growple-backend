package com.grow.common.event.payload;

import com.grow.common.event.EventPayload;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyJoinEventPayload  implements EventPayload {

    private Long studyId;
    private String title;
    private String content;
    private String level;
    private String category;
    private Long userId;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;
    private Long studyJoinMemberCount;
}
