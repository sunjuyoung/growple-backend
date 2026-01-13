package com.grow.common;


public record StudyCreateEvent(
        Long userId,
        Long studyId,
        String orderName,
        Integer amount,
        String nickname

) {
    public static StudyCreateEvent of(Long userId, Long studyId, String orderName, Integer amount, String nickname) {
        return new StudyCreateEvent(userId, studyId, orderName, amount, nickname);
    }
}
