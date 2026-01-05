package com.grow.payment.adapter.batch;

import com.grow.payment.application.required.SettlementItemRepository;
import com.grow.payment.application.required.SettlementRepository;
import com.grow.payment.domain.settlement.Settlement;
import com.grow.payment.domain.settlement.SettlementItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;

/**
 * Step 2 Reader: PENDING 상태의 Settlement 조회
 *
 * - DB에서 처리 대상 Settlement 조회
 * - 각 Settlement의 처리 대상 SettlementItem도 함께 로드
 */
@Slf4j
@Component
public class SettlementReader implements ItemReader<SettlementExecutionDto> {

    private final SettlementRepository settlementRepository;
    private final SettlementItemRepository settlementItemRepository;

    private Iterator<SettlementExecutionDto> settlementIterator;
    private boolean initialized = false;

    public SettlementReader(SettlementRepository settlementRepository,
                           SettlementItemRepository settlementItemRepository) {
        this.settlementRepository = settlementRepository;
        this.settlementItemRepository = settlementItemRepository;
    }

    @Override
    public SettlementExecutionDto read() {
        if (!initialized) {
            initialize();
        }

        if (settlementIterator != null && settlementIterator.hasNext()) {
            return settlementIterator.next();
        }

        // 다음 배치 실행을 위해 상태 초기화
        initialized = false;
        settlementIterator = null;
        return null;
    }

    private void initialize() {
        LocalDateTime now = LocalDateTime.now();
        log.info("정산 실행 대상 조회 시작 - 기준 시간: {}", now);

        // PENDING 또는 FAILED(재시도 가능) 상태의 Settlement 조회
        List<Settlement> settlementsToProcess = settlementRepository.findSettlementsToProcess(now);

        if (settlementsToProcess.isEmpty()) {
            log.info("처리 대상 정산 없음");
            settlementIterator = null;
            initialized = true;
            return;
        }

        log.info("처리 대상 정산 {} 건 조회됨", settlementsToProcess.size());

        // 각 Settlement에 대해 처리 대상 Item을 함께 로드
        List<SettlementExecutionDto> executionDtos = settlementsToProcess.stream()
                .map(settlement -> {
                    List<SettlementItem> items =
                            settlementItemRepository.findItemsToProcess(settlement.getId(), now);
                    return SettlementExecutionDto.of(settlement, items);
                })
                .filter(dto -> !dto.itemsToProcess().isEmpty())
                .toList();

        log.info("처리 대상 아이템이 있는 정산 {} 건", executionDtos.size());

        settlementIterator = executionDtos.iterator();
        initialized = true;
    }
}
