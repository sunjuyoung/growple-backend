package com.grow.study.adapter.webapi;

import com.grow.study.application.provided.StudyFinder;
import com.grow.study.application.required.dto.StudyWithMemberCountResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    @GetMapping("/{id}")
    public ResponseEntity<StudyWithMemberCountResponse> getStudyEnrollmentDetail(
            @Parameter(description = "스터디 ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("X-User-Id") Long userId) {
        StudyWithMemberCountResponse study = studyFinder.getStudyEnrollmentDetail(id,userId);

        return ResponseEntity.ok(study);
    }
}
