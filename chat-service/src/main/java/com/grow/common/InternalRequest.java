package com.grow.common;

public record InternalRequest(
        Long studyId,
        String roomName,
        Long userId
) {
}
