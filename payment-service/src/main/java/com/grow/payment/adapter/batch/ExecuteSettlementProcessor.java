package com.grow.payment.adapter.batch;

import com.grow.payment.application.required.MemberRestClient;
import com.grow.payment.application.required.SettlementItemRepository;
import com.grow.payment.application.required.SettlementRepository;
import com.grow.payment.application.required.StudyRestClient;
import com.grow.payment.domain.settlement.Settlement;
import com.grow.payment.domain.settlement.SettlementItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Step 2 Processor: 포인트 지급 실행
 *
 * - 각 SettlementItem에 대해 Member Service 호출하여 포인트 지급
 * - 성공/실패에 따라 Item 상태 업데이트
 * - 모든 Item 완료 시 Settlement 완료 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExecuteSettlementProcessor implements ItemProcessor<SettlementExecutionDto, SettlementExecutionDto> {

    private static final int MAX_RETRY_COUNT = 3;

    private final MemberRestClient memberRestClient;
    private final StudyRestClient studyRestClient;
    private final SettlementRepository settlementRepository;
    private final SettlementItemRepository settlementItemRepository;

    @Override
    public SettlementExecutionDto process(SettlementExecutionDto dto) {
        Settlement settlement = dto.settlement();
        List<SettlementItem> items = dto.itemsToProcess();
        LocalDateTime now = LocalDateTime.now();

        log.info("정산 실행 시작 - settlementId: {}, studyId: {}, 처리 대상 아이템: {} 건",
                settlement.getId(), settlement.getStudyId(), items.size());

        try {
            // Settlement 선점 (PENDING -> PROCESSING)
            settlement.claim(now);
            settlementRepository.save(settlement);

            int successCount = 0;
            int failCount = 0;

            // 각 아이템에 대해 포인트 지급
            for (SettlementItem item : items) {
                try {
                    processItem(item, now);
                    successCount++;
                } catch (Exception e) {
                    handleItemFailure(item, e, now);
                    failCount++;
                }
            }

            log.info("정산 아이템 처리 결과 - settlementId: {}, 성공: {}, 실패: {}",
                    settlement.getId(), successCount, failCount);

            // 모든 아이템 완료 여부 확인
            boolean allCompleted = settlementItemRepository.areAllItemsCompleted(settlement.getId());

            if (allCompleted) {
                // Settlement 완료 처리
                settlement.complete(now);
                settlementRepository.save(settlement);

                // Study Service에 정산 완료 통보
                try {
                    studyRestClient.markStudyAsSettled(settlement.getStudyId());
                    log.info("스터디 정산 완료 마킹 성공 - studyId: {}", settlement.getStudyId());
                } catch (Exception e) {
                    log.warn("스터디 정산 완료 마킹 실패 (정산은 완료됨) - studyId: {}, error: {}",
                            settlement.getStudyId(), e.getMessage());
                }

                log.info("정산 완료 - settlementId: {}, studyId: {}",
                        settlement.getId(), settlement.getStudyId());
            } else if (failCount > 0) {
                // 일부 실패 시 재시도 스케줄링
                handleSettlementPartialFailure(settlement, now);
            }

        } catch (Exception e) {
            log.error("정산 실행 중 오류 - settlementId: {}, error: {}",
                    settlement.getId(), e.getMessage());
            handleSettlementFailure(settlement, e, now);
        }

        return dto;
    }

    private void processItem(SettlementItem item, LocalDateTime now) {
        // 환급 금액이 0이면 지급 없이 완료 처리
        if (item.getRefundAmount() <= 0) {
            log.info("환급 금액 없음, 완료 처리 - itemId: {}, memberId: {}",
                    item.getId(), item.getMemberId());
            item.markPaid(now, null);
            settlementItemRepository.save(item);
            return;
        }

        log.info("포인트 지급 시작 - itemId: {}, memberId: {}, 환급액: {}",
                item.getId(), item.getMemberId(), item.getRefundAmount());

        // Member Service에 포인트 지급 요청
        String reason = String.format("스터디 정산 환급 (정산ID: %d)", item.getSettlementId());
        Long txId = memberRestClient.addPoints(
                item.getMemberId(),
                item.getRefundAmount(),
                reason
        );

        // 성공 처리
        item.markPaid(now, txId);
        settlementItemRepository.save(item);

        log.info("포인트 지급 완료 - itemId: {}, memberId: {}, 환급액: {}, txId: {}",
                item.getId(), item.getMemberId(), item.getRefundAmount(), txId);
    }

    private void handleItemFailure(SettlementItem item, Exception e, LocalDateTime now) {
        int nextRetryCount = item.getRetryCount() + 1;

        if (nextRetryCount > MAX_RETRY_COUNT) {
            log.error("아이템 최대 재시도 횟수 초과 - itemId: {}, memberId: {}",
                    item.getId(), item.getMemberId());
            item.markFailed(now, nextRetryCount, null, "Max retry exceeded: " + e.getMessage());
        } else {
            // 지수 백오프로 다음 재시도 시간 설정
            LocalDateTime nextRetryAt = now.plusMinutes((long) Math.pow(2, nextRetryCount));
            log.warn("아이템 처리 실패, 재시도 예정 - itemId: {}, nextRetryAt: {}, error: {}",
                    item.getId(), nextRetryAt, e.getMessage());
            item.markFailed(now, nextRetryCount, nextRetryAt, e.getMessage());
        }

        settlementItemRepository.save(item);
    }

    private void handleSettlementPartialFailure(Settlement settlement, LocalDateTime now) {
        int nextRetryCount = settlement.getRetryCount() + 1;
        LocalDateTime nextRetryAt = now.plusMinutes((long) Math.pow(2, nextRetryCount));

        settlement.markFailed(now, nextRetryCount, nextRetryAt, "Partial item failures");
        settlementRepository.save(settlement);

        log.info("정산 부분 실패, 재시도 예정 - settlementId: {}, nextRetryAt: {}",
                settlement.getId(), nextRetryAt);
    }

    private void handleSettlementFailure(Settlement settlement, Exception e, LocalDateTime now) {
        int nextRetryCount = settlement.getRetryCount() + 1;

        if (nextRetryCount > MAX_RETRY_COUNT) {
            log.error("정산 최대 재시도 횟수 초과 - settlementId: {}", settlement.getId());
            settlement.markFailed(now, nextRetryCount, null, "Max retry exceeded: " + e.getMessage());
        } else {
            LocalDateTime nextRetryAt = now.plusMinutes((long) Math.pow(2, nextRetryCount));
            settlement.markFailed(now, nextRetryCount, nextRetryAt, e.getMessage());
        }

        settlementRepository.save(settlement);
    }
}
