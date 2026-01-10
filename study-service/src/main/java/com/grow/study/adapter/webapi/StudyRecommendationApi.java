package com.grow.study.adapter.webapi;

import com.grow.study.adapter.webapi.dto.MemberInterestRequest;
import com.grow.study.adapter.webapi.dto.StudyRecommendationResponse;
import com.grow.study.application.StudyRecommendationService;
import com.grow.study.domain.study.Study;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 스터디 추천 API
 * pgvector 기반 유사 스터디 추천
 */
@Slf4j
@RestController
@RequestMapping("/api/studies")
@RequiredArgsConstructor
@Validated
@Tag(name = "Study Recommendation", description = "스터디 추천 API")
public class StudyRecommendationApi {

    private final StudyRecommendationService studyRecommendationService;

    /**
     * 유사 스터디 추천
     * 스터디 내용 기반 유사 스터디 검색
     */
    @GetMapping("/{studyId}/recommendations/similar")
    @Operation(
            summary = "유사 스터디 추천",
            description = "스터디 내용(제목, 소개, 커리큘럼)을 기반으로 유사한 스터디를 추천합니다. 모집 중인 스터디만 반환됩니다."
    )
    public ResponseEntity<List<StudyRecommendationResponse>> getSimilarStudies(
            @PathVariable @Parameter(description = "스터디 ID") Long studyId,
            @RequestParam(defaultValue = "3") @Positive @Parameter(description = "추천 개수") int limit
    ) {

        List<Study> similarStudies = studyRecommendationService.recommendSimilarStudies(studyId, limit);

        List<StudyRecommendationResponse> response = similarStudies.stream()
                .map(StudyRecommendationResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * 멤버 관심사 기반 스터디 추천
     */
    @PostMapping("/recommendations/by-interest")
    @Operation(
            summary = "멤버 관심사 기반 스터디 추천",
            description = "멤버의 관심사(소개)를 기반으로 적합한 스터디를 추천합니다. 모집 중인 스터디만 반환됩니다."
    )
    public ResponseEntity<List<StudyRecommendationResponse>> getStudiesByMemberInterest(
            @RequestHeader("X-User-Id") @Parameter(description = "유저 ID") Long userId,
            @Valid @RequestBody MemberInterestRequest request
    ) {

        List<Study> recommendedStudies = studyRecommendationService.recommendStudiesByMemberInterest(
                userId,
                request.getMemberIntroduction(),
                request.getLimit()
        );

        List<StudyRecommendationResponse> response = recommendedStudies.stream()
                .map(StudyRecommendationResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * 검색, 카테고리별 유사 스터디 추천
     */
    @GetMapping("/recommendations/by-category")
    @Operation(
            summary = "검색, 카테고리별 유사 스터디 추천",
            description = "특정 카테고리 내에서 검색 텍스트와 유사한 스터디를 추천합니다. 모집 중인 스터디만 반환됩니다."
    )
    public ResponseEntity<List<StudyRecommendationResponse>> getStudiesByCategory(
            @RequestParam @Parameter(description = "검색 텍스트") String query,
            @RequestParam @Parameter(description = "카테고리") String category,
            @RequestParam(defaultValue = "10") @Positive @Parameter(description = "추천 개수") int limit
    ) {

        List<Study> recommendedStudies = studyRecommendationService.recommendStudiesByCategory(
                query,
                category,
                limit
        );

        List<StudyRecommendationResponse> response = recommendedStudies.stream()
                .map(StudyRecommendationResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
