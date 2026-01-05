package com.grow.payment.adapter.batch;

import com.grow.payment.domain.settlement.Settlement;
import com.grow.payment.domain.settlement.SettlementItem;

import java.util.List;

/**
 * 정산 실행 처리용 DTO
 * Settlement과 처리 대상 Items를 함께 전달
 */
public record SettlementExecutionDto(
        Settlement settlement,
        List<SettlementItem> itemsToProcess
) {
    public static SettlementExecutionDto of(Settlement settlement, List<SettlementItem> items) {
        return new SettlementExecutionDto(settlement, items);
    }

    public Long getSettlementId() {
        return settlement.getId();
    }

    public Long getStudyId() {
        return settlement.getStudyId();
    }
}
