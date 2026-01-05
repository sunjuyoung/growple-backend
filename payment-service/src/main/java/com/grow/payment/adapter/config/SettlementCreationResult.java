package com.grow.payment.adapter.config;

import com.grow.payment.domain.settlement.Settlement;
import com.grow.payment.domain.settlement.SettlementItem;

import java.util.List;

/**
 * Settlement 생성 결과를 담는 래퍼 클래스
 * Processor에서 Writer로 전달할 때 Settlement과 Items를 함께 전달
 */
public record SettlementCreationResult(
        Settlement settlement,
        List<SettlementItem> items
) {
    public static SettlementCreationResult of(Settlement settlement, List<SettlementItem> items) {
        return new SettlementCreationResult(settlement, items);
    }
}
