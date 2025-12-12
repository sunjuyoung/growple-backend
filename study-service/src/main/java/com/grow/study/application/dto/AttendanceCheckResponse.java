package com.grow.study.application.dto;


import com.grow.study.domain.study.Attendance;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 출석 체크 응답 DTO
 */
@Schema(description = "출석 체크 응답")
public record AttendanceCheckResponse(
        @Schema(description = "출석 ID", example = "1")
        Long attendanceId,

        @Schema(description = "세션 ID", example = "1")
        Long sessionId,

        @Schema(description = "회원 ID", example = "1")
        Long memberId,

        @Schema(description = "출석 상태", example = "PRESENT")
        AttendanceStatus status,

        @Schema(description = "출석 체크 시간")
        LocalDateTime checkedAt,

        @Schema(description = "획득한 활동 점수", example = "5")
        Integer activityScore,

        @Schema(description = "비고")
        String note
) {
    public static AttendanceCheckResponse from(Attendance attendance) {
        return new AttendanceCheckResponse(
                attendance.getId(),
                attendance.getSession().getId(),
                attendance.getMemberId(),
                attendance.getStatus(),
                attendance.getCheckedAt(),
                attendance.getActivityScore(),
                attendance.getNote()
        );
    }
}
