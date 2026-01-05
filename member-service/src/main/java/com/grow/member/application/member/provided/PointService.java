package com.grow.member.application.member.provided;

import com.grow.member.adapter.webapi.dto.internal.PointRefundRequest;
import com.grow.member.adapter.webapi.dto.internal.PointRefundResponse;
import com.grow.member.application.member.required.MemberRepository;
import com.grow.member.domain.member.Member;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 포인트 관련 내부 서비스
 * Payment Service의 정산 배치에서 호출
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointService {

    private final MemberRepository memberRepository;

    /**
     * 포인트 환급 처리
     * 정산 배치에서 호출되며, 멱등성을 보장해야 함
     */
    @Transactional
    public PointRefundResponse refundPoint(Long memberId, PointRefundRequest request) {
        try {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new EntityNotFoundException("회원을 찾을 수 없습니다: " + memberId));

            if (!member.isActive()) {
                return PointRefundResponse.failure(
                        memberId,
                        request.settlementItemId(),
                        "비활성 회원에게는 포인트를 환급할 수 없습니다"
                );
            }

            member.refundPoint(request.amount());

            log.info("포인트 환급 완료 - memberId: {}, amount: {}, settlementItemId: {}, currentPoint: {}",
                    memberId, request.amount(), request.settlementItemId(), member.getPoint());

            return PointRefundResponse.success(
                    memberId,
                    request.amount(),
                    member.getPoint(),
                    request.settlementItemId()
            );

        } catch (EntityNotFoundException e) {
            log.error("포인트 환급 실패 - 회원 없음: memberId={}", memberId);
            return PointRefundResponse.failure(memberId, request.settlementItemId(), e.getMessage());
        } catch (Exception e) {
            log.error("포인트 환급 실패 - memberId: {}, error: {}", memberId, e.getMessage());
            return PointRefundResponse.failure(memberId, request.settlementItemId(), e.getMessage());
        }
    }

    /**
     * 포인트 조회
     */
    @Transactional(readOnly = true)
    public Integer getPoint(Long memberId) {
        return memberRepository.findById(memberId)
                .map(Member::getPoint)
                .orElseThrow(() -> new EntityNotFoundException("회원을 찾을 수 없습니다: " + memberId));
    }
}
