package com.grow.study.domain.study.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudyRegisterRequest {

    @NotBlank(message = "스터디 제목은 필수입니다")
    @Size(max = 50, message = "스터디 제목은 50자 이하여야 합니다")
    private String title;

    @NotBlank(message = "카테고리는 필수입니다")
    private String category;

    @NotBlank(message = "레벨은 필수입니다")
    private String level;

    @NotNull(message = "공개 여부는 필수입니다")
    private Boolean isPublic;

    @NotEmpty(message = "스터디 진행 요일은 최소 1개 이상이어야 합니다")
    private Set<String> daysOfWeek;

    @NotNull(message = "보증금은 필수입니다")
    @Min(value = 5000, message = "보증금은 최소 5,000P 이상이어야 합니다")
    @Max(value = 50000, message = "보증금은 최대 50,000P 이하여야 합니다")
    private Integer deposit;

    @NotNull(message = "최소 인원은 필수입니다")
    @Min(value = 1, message = "최소 인원은 1명 이상이어야 합니다")
    private Integer minMembers;

    @NotNull(message = "최대 인원은 필수입니다")
    @Min(value = 2, message = "최대 인원은 2명 이상이어야 합니다")
    @Max(value = 50, message = "최대 인원은 50명 이하여야 합니다")
    private Integer maxMembers;

    @NotNull(message = "시작일은 필수입니다")
    private String startDate; // LocalDate로 파싱됨

    @NotNull(message = "종료일은 필수입니다")
    private String endDate;

    @NotNull(message = "시작 시간은 필수입니다")
    private String startTime; // LocalTime으로 파싱됨

    @NotNull(message = "종료 시간은 필수입니다")
    private String endTime;

    @Size(max = 2000, message = "스터디 소개는 2000자 이하여야 합니다")
    private String introduction;

    @Size(max = 2000, message = "진행 방식은 2000자 이하여야 합니다")
    private String curriculum;

    @Size(max = 500, message = "한마디는 500자 이하여야 합니다")
    private String leaderMessage;

    private Integer totalSessions;
}
