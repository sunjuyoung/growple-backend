package com.grow.study.adapter.webapi;

import com.grow.study.adapter.scheduler.AttendanceScheduler;
import com.grow.study.adapter.scheduler.RecruitmentDeadlineScheduler;
import com.grow.study.adapter.scheduler.StudyCompletionScheduler;
import com.grow.study.adapter.scheduler.StudyStartScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 스케줄러 테스트 및 즉시 실행용 트리거 API
 * 개발/테스트 환경에서만 사용을 권장합니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/scheduler")
@Tag(name = "Scheduler Trigger", description = "스케줄러 수동 트리거 API (테스트용)")
public class SchedulerTriggerApi {

    private final RecruitmentDeadlineScheduler recruitmentDeadlineScheduler;
    private final StudyStartScheduler studyStartScheduler;
    private final StudyCompletionScheduler studyCompletionScheduler;
    private final AttendanceScheduler attendanceScheduler;

    @Operation(
            summary = "모집 마감 처리 트리거",
            description = "RECRUITING 상태이면서 모집 종료일이 오늘인 스터디를 처리합니다. (매일 00:05 실행)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "트리거 성공")
    })
    @PostMapping("/trigger/recruitment-deadline")
    public ResponseEntity<Map<String, Object>> triggerRecruitmentDeadline() {
        log.info("모집 마감 처리 스케줄러 수동 트리거 요청");
        LocalDateTime startTime = LocalDateTime.now();

        recruitmentDeadlineScheduler.processRecruitmentDeadline();

        return ResponseEntity.ok(Map.of(
                "scheduler", "RecruitmentDeadlineScheduler",
                "triggeredAt", startTime,
                "completedAt", LocalDateTime.now(),
                "message", "모집 마감 처리 완료"
        ));
    }

    @Operation(
            summary = "스터디 시작 처리 트리거",
            description = "RECRUIT_CLOSED 상태이면서 시작일이 오늘인 스터디를 IN_PROGRESS로 전환합니다. (매일 00:10 실행)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "트리거 성공")
    })
    @PostMapping("/trigger/study-start")
    public ResponseEntity<Map<String, Object>> triggerStudyStart() {
        log.info("스터디 시작 처리 스케줄러 수동 트리거 요청");
        LocalDateTime startTime = LocalDateTime.now();

        studyStartScheduler.processStudyStart();

        return ResponseEntity.ok(Map.of(
                "scheduler", "StudyStartScheduler",
                "triggeredAt", startTime,
                "completedAt", LocalDateTime.now(),
                "message", "스터디 시작 처리 완료"
        ));
    }

    @Operation(
            summary = "스터디 종료 처리 트리거",
            description = "IN_PROGRESS 상태이면서 종료일이 지난 스터디를 COMPLETED로 전환합니다. (매일 00:20 실행)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "트리거 성공")
    })
    @PostMapping("/trigger/study-completion")
    public ResponseEntity<Map<String, Object>> triggerStudyCompletion() {
        log.info("스터디 종료 처리 스케줄러 수동 트리거 요청");
        LocalDateTime startTime = LocalDateTime.now();

        studyCompletionScheduler.processStudyCompletion();

        return ResponseEntity.ok(Map.of(
                "scheduler", "StudyCompletionScheduler",
                "triggeredAt", startTime,
                "completedAt", LocalDateTime.now(),
                "message", "스터디 종료 처리 완료"
        ));
    }

    @Operation(
            summary = "출석 마감 처리 트리거",
            description = "출석 체크 시간이 지난 세션의 미출석 멤버를 결석 처리합니다. (5분마다 실행)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "트리거 성공")
    })
    @PostMapping("/trigger/attendance")
    public ResponseEntity<Map<String, Object>> triggerAttendance() {
        log.info("출석 마감 처리 스케줄러 수동 트리거 요청");
        LocalDateTime startTime = LocalDateTime.now();

        attendanceScheduler.processExpiredSessions();

        return ResponseEntity.ok(Map.of(
                "scheduler", "AttendanceScheduler",
                "triggeredAt", startTime,
                "completedAt", LocalDateTime.now(),
                "message", "출석 마감 처리 완료"
        ));
    }

    @Operation(
            summary = "전체 스케줄러 트리거",
            description = "모든 스케줄러를 순차적으로 실행합니다. (모집마감 → 스터디시작 → 스터디종료 → 출석마감)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "트리거 성공")
    })
    @PostMapping("/trigger/all")
    public ResponseEntity<Map<String, Object>> triggerAll() {
        log.info("전체 스케줄러 수동 트리거 요청");
        LocalDateTime startTime = LocalDateTime.now();

        recruitmentDeadlineScheduler.processRecruitmentDeadline();
        studyStartScheduler.processStudyStart();
        studyCompletionScheduler.processStudyCompletion();
        attendanceScheduler.processExpiredSessions();

        return ResponseEntity.ok(Map.of(
                "scheduler", "ALL",
                "triggeredAt", startTime,
                "completedAt", LocalDateTime.now(),
                "message", "전체 스케줄러 처리 완료",
                "executionOrder", new String[]{
                        "RecruitmentDeadlineScheduler",
                        "StudyStartScheduler",
                        "StudyCompletionScheduler",
                        "AttendanceScheduler"
                }
        ));
    }
}
