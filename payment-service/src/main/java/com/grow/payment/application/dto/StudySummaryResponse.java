package com.grow.payment.application.dto;

public record StudySummaryResponse (
        Long id,
        String status,
        Integer amount,
        Long leaderId
){
}
