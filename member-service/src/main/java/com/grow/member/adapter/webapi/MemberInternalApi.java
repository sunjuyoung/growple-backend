package com.grow.member.adapter.webapi;

import com.grow.member.adapter.webapi.dto.internal.PointRefundRequest;
import com.grow.member.adapter.webapi.dto.internal.PointRefundResponse;
import com.grow.member.application.member.provided.PointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 회원 Internal API
 * Payment Service의 정산 배치에서 호출
 * 
 * 주의: 이 API는 내부 서비스 간 통신용으로, Gateway를 통하지 않고 직접 호출됨
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/members")
@Tag(name = "Member Internal API", description = "정산용 내부 API (서비스 간 통신)")
public class MemberInternalApi {

    private final PointService pointService;

    /**
     * 포인트 환급 처리
     * Payment Service의 정산 배치에서 호출
     */
    @Operation(
            summary = "포인트 환급",
            description = "정산 결과에 따라 회원에게 포인트를 환급합니다."
    )
    @PostMapping("/{memberId}/refund")
    public ResponseEntity<PointRefundResponse> refundPoint(
            @PathVariable Long memberId,
            @Valid @RequestBody PointRefundRequest request
    ) {
        log.info("포인트 환급 요청 - memberId: {}, amount: {}, settlementItemId: {}",
                memberId, request.amount(), request.settlementItemId());

        PointRefundResponse response = pointService.refundPoint(memberId, request);

        if (response.success()) {
            log.info("포인트 환급 완료 - memberId: {}, amount: {}", memberId, request.amount());
            return ResponseEntity.ok(response);
        } else {
            log.warn("포인트 환급 실패 - memberId: {}, reason: {}", memberId, response.message());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 포인트 조회
     */
    @Operation(
            summary = "포인트 조회",
            description = "회원의 현재 포인트를 조회합니다."
    )
    @GetMapping("/{memberId}/point")
    public ResponseEntity<Integer> getPoint(@PathVariable Long memberId) {
        Integer point = pointService.getPoint(memberId);
        return ResponseEntity.ok(point);
    }
}
