package com.grow.study.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 내 스터디 목록 응답 DTO
 * 참여중, 예정, 완료 3개 탭의 스터디 목록을 포함
 */
@Getter
@Builder
public class MyStudiesResponse {

    private List<MyStudySummary> participating;  // 참여중 (진행 중)
    private List<MyStudySummary> upcoming;       // 예정 (모집 중 + 모집 마감)
    private List<MyStudySummary> completed;      // 완료

    public static MyStudiesResponse of(
            List<MyStudySummary> participating,
            List<MyStudySummary> upcoming,
            List<MyStudySummary> completed
    ) {
        return MyStudiesResponse.builder()
                .participating(participating)
                .upcoming(upcoming)
                .completed(completed)
                .build();
    }
}
