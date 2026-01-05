package com.grow.payment.adapter.config;

import com.grow.payment.application.required.SettlementItemRepository;
import com.grow.payment.application.required.SettlementRepository;
import com.grow.payment.domain.settlement.Settlement;
import com.grow.payment.domain.settlement.SettlementItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Step 1 Writer: Settlement + SettlementItem DB 저장
 *
 * - Settlement 저장 후 ID 획득
 * - SettlementItem에 settlementId 설정 후 저장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementWriter implements ItemWriter<SettlementCreationResult> {

    private final SettlementRepository settlementRepository;
    private final SettlementItemRepository settlementItemRepository;

    @Override
    public void write(Chunk<? extends SettlementCreationResult> chunk) {
        log.info("정산 저장 시작 - {} 건", chunk.size());

        for (SettlementCreationResult result : chunk) {
            try {
                // 1. Settlement 저장
                Settlement savedSettlement = settlementRepository.save(result.settlement());
                Long settlementId = savedSettlement.getId();

                log.info("Settlement 저장 완료 - settlementId: {}, studyId: {}",
                        settlementId, savedSettlement.getStudyId());

                // 2. SettlementItem에 settlementId 설정 후 저장
                List<SettlementItem> items = result.items();
                for (SettlementItem item : items) {
                    setSettlementId(item, settlementId);
                }

                List<SettlementItem> savedItems = settlementItemRepository.saveAll(items);

                log.info("SettlementItem 저장 완료 - settlementId: {}, 아이템 수: {}",
                        settlementId, savedItems.size());

            } catch (Exception e) {
                log.error("정산 저장 실패 - studyId: {}, error: {}",
                        result.settlement().getStudyId(), e.getMessage());
                throw e;
            }
        }

        log.info("정산 저장 완료 - {} 건", chunk.size());
    }

    /**
     * SettlementItem의 settlementId 필드를 리플렉션으로 설정
     * (엔티티가 setter를 제공하지 않는 경우)
     */
    private void setSettlementId(SettlementItem item, Long settlementId) {
        try {
            Field field = SettlementItem.class.getDeclaredField("settlementId");
            field.setAccessible(true);
            field.set(item, settlementId);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("settlementId 설정 실패", e);
        }
    }
}
