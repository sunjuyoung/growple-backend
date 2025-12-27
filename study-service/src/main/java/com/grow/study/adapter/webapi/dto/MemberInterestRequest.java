package com.grow.study.adapter.webapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 멤버 관심사 기반 추천 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberInterestRequest {

    @NotBlank(message = "멤버 소개는 필수입니다")
    private String memberIntroduction;

    @Positive(message = "추천 개수는 양수여야 합니다")
    private Integer limit = 10;
}
