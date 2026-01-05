package com.grow.study.adapter.webapi;

import com.grow.study.adapter.webapi.dto.settlement.CompletedStudyForSettlementResponse;
import com.grow.study.application.provided.StudySettlementQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 정산용 Internal API
 * Payment Service의 배치에서 호출
 * 
 * 주의: 이 API는 내부 서비스 간 통신용으로, Gateway를 통하지 않고 직접 호출됨
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/studies")
@Tag(name = "Study Internal API", description = "정산용 내부 API (서비스 간 통신)")
public class StudyInternalApi {

    private final StudySettlementQueryService settlementQueryService;

    /**
     * 정산 대상 스터디 목록 조회
     * - 상태가 COMPLETED이면서 아직 정산되지 않은(SETTLED가 아닌) 스터디
     * - 참가자 정보(보증금, 출결)를 함께 반환
     */
    @Operation(
            summary = "정산 대상 스터디 조회",
            description = "COMPLETED 상태의 스터디 중 정산 대상을 조회합니다. 배치에서 사용됩니다."
    )
    @GetMapping("/completed-for-settlement")
    public ResponseEntity<List<CompletedStudyForSettlementResponse>> getCompletedStudiesForSettlement(
            @RequestParam(defaultValue = "100") int limit
    ) {
        log.info("정산 대상 스터디 조회 요청 - limit: {}", limit);
        
        List<CompletedStudyForSettlementResponse> studies = 
                settlementQueryService.findCompletedStudiesForSettlement(limit);
        
        log.info("정산 대상 스터디 {}건 조회 완료", studies.size());
        return ResponseEntity.ok(studies);
    }

    /**
     * 스터디 정산 완료 처리
     * Payment Service에서 정산 완료 후 호출
     */
    @Operation(
            summary = "스터디 정산 완료 처리",
            description = "스터디 상태를 SETTLED로 변경합니다."
    )
    @PostMapping("/{studyId}/mark-settled")
    public ResponseEntity<Void> markStudyAsSettled(@PathVariable Long studyId) {
        log.info("스터디 정산 완료 처리 요청 - studyId: {}", studyId);
        
        settlementQueryService.markAsSettled(studyId);
        
        log.info("스터디 {} 정산 완료 처리됨", studyId);
        return ResponseEntity.ok().build();
    }
}
