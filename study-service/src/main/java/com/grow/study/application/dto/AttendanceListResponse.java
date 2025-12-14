package com.grow.study.application.dto;


import com.grow.study.domain.study.Attendance;
import com.grow.study.domain.study.AttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 출석 목록 응답 DTO
 */
@Schema(description = "출석 목록 응답")
public record AttendanceListResponse(
        @Schema(description = "출석 목록")
        List<AttendanceInfo> attendances,

        @Schema(description = "출석 통계")
        AttendanceStatistics statistics
) {
    public static AttendanceListResponse of(List<Attendance> attendances, AttendanceStatistics statistics) {
        List<AttendanceInfo> attendanceInfos = attendances.stream()
                .map(AttendanceInfo::from)
                .toList();

        return new AttendanceListResponse(attendanceInfos, statistics);
    }

    @Schema(description = "출석 정보")
    public record AttendanceInfo(
            @Schema(description = "출석 ID", example = "1")
            Long attendanceId,

            @Schema(description = "세션 번호", example = "1")
            Integer sessionNumber,

            @Schema(description = "세션 일자", example = "2024-01-15")
            LocalDate sessionDate,

            @Schema(description = "출석 상태", example = "PRESENT")
            AttendanceStatus status,

            @Schema(description = "출석 체크 시간")
            LocalDateTime checkedAt,

            @Schema(description = "획득한 활동 점수", example = "5")
            Integer activityScore,

            @Schema(description = "비고")
            String note
    ) {
        public static AttendanceInfo from(Attendance attendance) {
            return new AttendanceInfo(
                    attendance.getId(),
                    attendance.getSession().getSessionNumber(),
                    attendance.getSession().getSessionDate(),
                    attendance.getStatus(),
                    attendance.getCheckedAt(),
                    attendance.getActivityScore(),
                    attendance.getNote()
            );
        }
    }

    @Schema(description = "출석 통계")
    public record AttendanceStatistics(
            @Schema(description = "총 세션 수", example = "10")
            Integer totalSessions,

            @Schema(description = "출석 횟수", example = "8")
            Integer presentCount,

            @Schema(description = "결석 횟수", example = "2")
            Integer absentCount,

            @Schema(description = "지각 횟수", example = "0")
            Integer lateCount,

            @Schema(description = "출석률 (%)", example = "80.0")
            Double attendanceRate
    ) {
        public static AttendanceStatistics from(List<Attendance> attendances) {
            int totalSessions = attendances.size();
            long presentCount = attendances.stream()
                    .filter(a -> a.getStatus() == AttendanceStatus.PRESENT)
                    .count();
            long absentCount = attendances.stream()
                    .filter(a -> a.getStatus() == AttendanceStatus.ABSENT)
                    .count();
            long lateCount = attendances.stream()
                    .filter(a -> a.getStatus() == AttendanceStatus.LATE)
                    .count();

            double attendanceRate = totalSessions > 0
                    ? (double) presentCount / totalSessions * 100
                    : 0.0;

            return new AttendanceStatistics(
                    totalSessions,
                    (int) presentCount,
                    (int) absentCount,
                    (int) lateCount,
                    Math.round(attendanceRate * 100) / 100.0
            );
        }
    }
}
