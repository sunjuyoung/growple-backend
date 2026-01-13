package com.grow.common;

public record InternalRequest(
        Long studyId,
        String roomName,
        Long userId,
        String nickname
) {
    public InternalRequest create(
            Long studyId,
            String roomName,
            Long userId,
            String nickname
    ) { return new InternalRequest(studyId, roomName, userId,nickname); }
}
