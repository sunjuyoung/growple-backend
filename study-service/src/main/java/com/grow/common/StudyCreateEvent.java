package com.grow.common;

public record StudyCreateEvent(
        Long userId,
        Long studyId,
        String orderName,
        Integer amount

) {
    public static StudyCreateEvent of(Long userId, Long studyId, String orderName, Integer amount) {
        return new StudyCreateEvent(userId, studyId, orderName, amount);
    }
}
