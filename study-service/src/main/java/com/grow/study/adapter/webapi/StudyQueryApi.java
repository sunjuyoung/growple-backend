package com.grow.study.adapter.webapi;

import com.grow.study.adapter.persistence.dto.CursorResult;
import com.grow.study.adapter.persistence.dto.StudyListResponse;
import com.grow.study.application.dto.MyStudiesResponse;
import com.grow.study.application.dto.StudyDashboardResponse;
import com.grow.study.application.provided.StudyFinder;
import com.grow.study.application.required.dto.StudySummaryResponse;
import com.grow.study.application.required.dto.StudyWithMemberCountResponse;
import com.grow.study.domain.study.StudyCategory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/study")
@Tag(name = "Study Query", description = "스터디 조회 API")
public class StudyQueryApi {

    private final StudyFinder studyFinder;

    @Operation(
            summary = "스터디 상세 조회",
            description = "스터디 ID로 스터디 상세 정보와 멤버 수를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = StudyWithMemberCountResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "스터디를 찾을 수 없음",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content
            )
    })
    @GetMapping("/get/{id}")
    public ResponseEntity<StudyWithMemberCountResponse> getStudyEnrollmentDetail(
            @PathVariable Long id) {
        StudyWithMemberCountResponse study = studyFinder.getStudyEnrollmentDetail(id);

        return ResponseEntity.ok(study);
    }

    @Operation(
            summary = "스터디 목록 조회",
            description = "필터 조건에 따라 스터디 목록을 조회합니다. 모든 필터는 선택사항입니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공"
            )
    })
    @GetMapping("/pages")
    public ResponseEntity<Page<StudyListResponse>> getStudyList(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) StudyCategory category,
            @RequestParam(required = false) Integer minDepositAmount,
            @RequestParam(required = false) Integer maxDepositAmount,
            @Parameter(description = "정렬 타입 (LATEST: 최신순, DEADLINE_SOON: 마감임박순)")
            @RequestParam(required = false) String sortType,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<StudyListResponse> result = studyFinder.getStudyList(
                level,
                category,
                minDepositAmount,
                maxDepositAmount,
                sortType,
                pageable
        );

        return ResponseEntity.ok(result);
    }

    @GetMapping("/list")
    public ResponseEntity<CursorResult<StudyListResponse>> getStudyListByCursor(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) StudyCategory category,
            @RequestParam(required = false) Integer minDepositAmount,
            @RequestParam(required = false) Integer maxDepositAmount,
            @Parameter(description = "정렬 타입 (LATEST: 최신순, DEADLINE_SOON: 마감임박순)")
            @RequestParam(required = false) String sortType,
            @RequestParam(required = false) String cursor
    ) {
        CursorResult<StudyListResponse> result = studyFinder.getStudyListByCursor(
                level,
                category,
                minDepositAmount,
                maxDepositAmount,
                sortType,
                cursor
        );

        return ResponseEntity.ok(result);
    }


    @GetMapping("/board/{id}")
    public ResponseEntity<StudyDashboardResponse> getStudyDashBoardDetail(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        StudyDashboardResponse studyDashboard = studyFinder.getStudyDashboard(id, userId);

        return ResponseEntity.ok(studyDashboard);
    }


    @GetMapping("/summary/{id}")
    public ResponseEntity<StudySummaryResponse> getStudySimpleDetail(
            @Parameter(description = "스터디 ID", required = true)
            @PathVariable Long id) {
        StudySummaryResponse response = studyFinder.getStudySimpleDetail(id);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "내 스터디 목록 조회",
            description = "현재 로그인한 사용자의 스터디 목록을 참여중, 예정, 완료로 분류하여 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = MyStudiesResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content
            )
    })
    @GetMapping("/my")
    public ResponseEntity<MyStudiesResponse> getMyStudies(
            @RequestHeader("X-User-Id") Long userId) {
        MyStudiesResponse response = studyFinder.getMyStudies(userId);
        return ResponseEntity.ok(response);
    }
}
