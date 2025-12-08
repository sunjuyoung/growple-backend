package com.grow.study.adapter.webapi.dto;

public record StudyEnrollmentRequest (
        Long studyId,
        Long userId,
        Integer depositAmount
){
}
