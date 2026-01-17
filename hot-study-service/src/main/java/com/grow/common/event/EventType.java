package com.grow.common.event;


import com.grow.common.event.payload.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@RequiredArgsConstructor
public enum EventType {
    STUDY_JOIN(StudyJoinEventPayload.class, Topic.STUDY_JOIN),
    STUDY_LEAVE(StudyLeaveEventPayload.class, Topic.STUDY_JOIN),
    STUDY_FAVORITE(StudyFavoriteEventPayload.class, Topic.STUDY_FAVORITE),
    STUDY_UNFAVORITE(StudyUnFavoriteEventPayload.class, Topic.STUDY_FAVORITE),
    STUDY_VIEWED(StudyViewEventPayload.class, Topic.STUDY_VIEW)
    ;

    private final Class<? extends EventPayload> payloadClass;
    private final String topic;

    public static EventType from(String type) {
        try {
            return valueOf(type);
        } catch (Exception e) {
            log.error("[EventType.from] type={}", type, e);
            return null;
        }
    }

    public static class Topic {
        public static final String STUDY_JOIN = "study-join";
        public static final String STUDY_FAVORITE = "study-favorite";
        public static final String STUDY_VIEW = "study-view";
    }
}
