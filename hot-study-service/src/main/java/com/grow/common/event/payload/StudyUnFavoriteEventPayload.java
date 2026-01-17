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
public class StudyUnFavoriteEventPayload  implements EventPayload {

    private Long studyFavoriteId;
    private Long studyId;
    private Long userId;
    private LocalDateTime createdAt;
    private Long studyFavoriteCount;


}
