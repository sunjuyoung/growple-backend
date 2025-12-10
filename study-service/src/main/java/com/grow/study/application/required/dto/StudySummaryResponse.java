package com.grow.study.application.required.dto;

public record StudySummaryResponse(
        Long id,
        String status,
        Integer amount,
        Long leaderId
) {
    public static StudySummaryResponse of(Long id, String status, Integer amount,  Long leaderId) {
        return new StudySummaryResponse(id, status, amount, leaderId);
    }
}
