package com.grow.study.adapter.webapi;

import com.grow.study.application.StudyVectorBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 스터디 벡터 배치 API
 * 관리자용 - 기존 스터디들의 Document 일괄 생성
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/studies/vector")
@RequiredArgsConstructor
@Tag(name = "Study Vector Batch (Admin)", description = "스터디 벡터 배치 API (관리자)")
public class StudyVectorBatchApi {

    private final StudyVectorBatchService studyVectorBatchService;

    /**
     * 모집 중인 모든 스터디의 Document 생성
     * 초기 마이그레이션 또는 수동 실행용
     */
    @PostMapping("/batch/create-recruiting")
    @Operation(
            summary = "모집 중인 스터디 Document 일괄 생성",
            description = "모집 중인 모든 스터디의 pgvector Document를 일괄 생성합니다. " +
                    "초기 마이그레이션 또는 데이터 동기화 시 사용하세요."
    )
    public ResponseEntity<Map<String, String>> createRecruitingDocuments() {
        log.info("POST /api/v1/admin/studies/vector/batch/create-recruiting - Batch creation started");

        studyVectorBatchService.createDocumentsForAllRecruitingStudies();

        return ResponseEntity.ok(Map.of(
                "message", "배치 작업이 완료되었습니다.",
                "status", "success"
        ));
    }

    /**
     * 모든 스터디의 Document 재생성
     */
    @PostMapping("/batch/recreate-all")
    @Operation(
            summary = "전체 스터디 Document 재생성",
            description = "모든 스터디의 pgvector Document를 재생성합니다. " +
                    "주의: 시간이 오래 걸릴 수 있습니다."
    )
    public ResponseEntity<Map<String, String>> recreateAllDocuments() {
        log.warn("POST /api/v1/admin/studies/vector/batch/recreate-all - Recreation started");

        studyVectorBatchService.recreateAllDocuments();

        return ResponseEntity.ok(Map.of(
                "message", "재생성 작업이 완료되었습니다.",
                "status", "success"
        ));
    }
}
