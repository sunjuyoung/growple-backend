package com.grow.study.application.required.dto;

public record MemberSummaryResponse(
        Long id,
        String nickname,
        String profileImageUrl,
        int level
) {

}