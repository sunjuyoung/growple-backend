package com.grow.study.application.dto;


import com.grow.study.domain.study.Attendance;
import com.grow.study.domain.study.AttendanceStatus;
import com.grow.study.domain.study.Session;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;


/**
 * 세션별 출석 현황 응답 DTO
 */
@Schema(description = "세션별 출석 현황 응답")
public record SessionAttendanceResponse(
        @Schema(description = "세션 정보")
        SessionInfo session,

        @Schema(description = "출석 목록")
        List<MemberAttendance> attendances,

        @Schema(description = "출석 통계")
        SessionStatistics statistics
) {
    public static SessionAttendanceResponse of(Session session, List<Attendance> attendances) {
        SessionInfo sessionInfo = SessionInfo.from(session);
        List<MemberAttendance> memberAttendances = attendances.stream()
                .map(MemberAttendance::from)
                .toList();
        SessionStatistics statistics = SessionStatistics.from(session);

        return new SessionAttendanceResponse(sessionInfo, memberAttendances, statistics);
    }

    @Schema(description = "세션 정보")
    public record SessionInfo(
            @Schema(description = "세션 ID", example = "1")
            Long sessionId,

            @Schema(description = "세션 번호", example = "1")
            Integer sessionNumber,

            @Schema(description = "세션 일자", example = "2024-01-15")
            LocalDate sessionDate,

            @Schema(description = "시작 시간", example = "14:00")
            LocalTime startTime,

            @Schema(description = "종료 시간", example = "16:00")
            LocalTime endTime,

            @Schema(description = "출석 체크 시작 시간")
            LocalDateTime attendanceCheckStartTime,

            @Schema(description = "출석 체크 종료 시간")
            LocalDateTime attendanceCheckEndTime,

            @Schema(description = "출석 체크 가능 여부")
            Boolean isAttendanceCheckAvailable
    ) {
        public static SessionInfo from(Session session) {
            return new SessionInfo(
                    session.getId(),
                    session.getSessionNumber(),
                    session.getSessionDate(),
                    session.getStartTime(),
                    session.getEndTime(),
                    session.getAttendanceCheckStartTime(),
                    session.getAttendanceCheckEndTime(),
                    session.isAttendanceCheckAvailable()
            );
        }
    }

    @Schema(description = "회원 출석 정보")
    public record MemberAttendance(
            @Schema(description = "회원 ID", example = "1")
            Long memberId,

            @Schema(description = "출석 상태", example = "PRESENT")
            AttendanceStatus status,

            @Schema(description = "출석 체크 시간")
            LocalDateTime checkedAt,

            @Schema(description = "비고")
            String note
    ) {
        public static MemberAttendance from(Attendance attendance) {
            return new MemberAttendance(
                    attendance.getMemberId(),
                    attendance.getStatus(),
                    attendance.getCheckedAt(),
                    attendance.getNote()
            );
        }
    }

    @Schema(description = "세션 통계")
    public record SessionStatistics(
            @Schema(description = "출석 인원", example = "8")
            Integer attendanceCount,

            @Schema(description = "결석 인원", example = "2")
            Integer absenceCount,

            @Schema(description = "출석률 (%)", example = "80.0")
            Double attendanceRate
    ) {
        public static SessionStatistics from(Session session) {
            return new SessionStatistics(
                    session.getAttendanceCount(),
                    session.getAbsenceCount(),
                    Math.round(session.getAttendanceRate() * 100) / 100.0
            );
        }
    }
}
