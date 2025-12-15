package com.grow.study.adapter.webapi;


import com.grow.study.application.AttendanceService;
import com.grow.study.application.dto.AttendanceCheckRequest;
import com.grow.study.application.dto.AttendanceCheckResponse;
import com.grow.study.application.dto.AttendanceListResponse;
import com.grow.study.application.dto.SessionAttendanceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 출석 체크 API
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/study/attendance")
@Tag(name = "Attendance", description = "출석 체크 API")
public class AttendanceApi {

    private final AttendanceService attendanceService;

    @Operation(
            summary = "출석 체크",
            description = "스터디 세션에 출석 체크를 합니다. 출석 체크 가능 시간(세션 시작 30분 전 ~ 10분 후)에만 가능합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "출석 체크 성공",
                    content = @Content(schema = @Schema(implementation = AttendanceCheckResponse.class))
            )
    })
    @PostMapping("/check")
    public ResponseEntity<AttendanceCheckResponse> checkAttendance(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody AttendanceCheckRequest request
    ) {
        AttendanceCheckResponse response = attendanceService.checkAttendance(
                request.sessionId(),
                request.studyId(),
                userId
        );

        log.info("출석 체크 완료 - userId: {}, sessionId: {}", userId, request.sessionId());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "내 출석 목록 조회",
            description = "특정 스터디에서 내 출석 목록과 통계를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = AttendanceListResponse.class))
            )
    })
    @GetMapping("/my/{studyId}")
    public ResponseEntity<AttendanceListResponse> getMyAttendanceList(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long studyId
    ) {
        AttendanceListResponse response = attendanceService.getAttendanceList(studyId, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "세션별 출석 현황 조회 (스터디장 전용)",
            description = "특정 세션의 전체 출석 현황을 조회합니다. 스터디장만 접근 가능합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = SessionAttendanceResponse.class))
            )
    })
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<SessionAttendanceResponse> getSessionAttendance(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long sessionId
    ) {
        SessionAttendanceResponse response = attendanceService.getSessionAttendance(sessionId, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "결석 처리 (자동)",
            description = "출석 체크 시간이 지난 후 미체크 회원을 자동으로 결석 처리합니다. (스케줄러 또는 관리자용)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "처리 성공"
            )
    })
    @PostMapping("/process-absences/{sessionId}")
    public ResponseEntity<Void> processAbsences(
            @PathVariable Long sessionId
    ) {
        attendanceService.processAbsences(sessionId);
        log.info("결석 처리 완료 - sessionId: {}", sessionId);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "출석 상태 수정 (스터디장 전용)",
            description = "출석 상태를 수정합니다. 스터디장만 접근 가능합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "수정 성공"
            )
    })
    @PutMapping("/{attendanceId}")
    public ResponseEntity<Void> updateAttendanceStatus(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long attendanceId,
            @RequestParam String status,
            @RequestParam(required = false) String note
    ) {
        attendanceService.updateAttendanceStatus(attendanceId, userId, status, note);
        log.info("출석 상태 수정 - attendanceId: {}, status: {}", attendanceId, status);
        return ResponseEntity.ok().build();
    }
}
