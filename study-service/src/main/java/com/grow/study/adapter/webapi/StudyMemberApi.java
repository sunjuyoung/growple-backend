package com.grow.study.adapter.webapi;

import com.grow.study.application.dto.StudyMemberListResponse;
import com.grow.study.application.provided.StudyFinder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/study")
@Tag(name = "Study Member", description = "스터디 멤버 관리 API")
public class StudyMemberApi {

    private final StudyFinder studyFinder;

    @Operation(
            summary = "스터디 멤버 리스트 조회",
            description = "특정 스터디의 활성 멤버 목록을 조회합니다. 멤버 정보(닉네임, 프로필, 레벨)를 포함합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = StudyMemberListResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "스터디를 찾을 수 없음",
                    content = @Content
            )
    })
    @GetMapping("/{studyId}/members")
    public ResponseEntity<StudyMemberListResponse> getStudyMembers(
            @Parameter(description = "스터디 ID", required = true)
            @PathVariable Long studyId
    ) {
        StudyMemberListResponse response = studyFinder.getStudyMembers(studyId);
        return ResponseEntity.ok(response);
    }
}
