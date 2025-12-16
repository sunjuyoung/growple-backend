package com.grow.common;

public record InternalRequest(
        Long studyId,
        String roomName,
        Long userId
) {
    public InternalRequest create(
            Long studyId,
            String roomName,
            Long userId
    ) { return new InternalRequest(studyId, roomName, userId); }
}
