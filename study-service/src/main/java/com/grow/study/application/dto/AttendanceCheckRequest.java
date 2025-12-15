package com.grow.study.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 출석 체크 요청 DTO
 */
@Schema(description = "출석 체크 요청")
public record AttendanceCheckRequest(
        @NotNull(message = "세션 ID는 필수입니다")
        @Schema(description = "세션 ID", example = "1")
        Long sessionId,
        Long studyId
) {
}
