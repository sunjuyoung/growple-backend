package com.grow.payment.adapter.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * Step 2 Writer: 정산 실행 결과 처리
 *
 * 실제 저장 로직은 Processor에서 이미 수행됨
 * Writer는 최종 로깅만 담당
 */
@Slf4j
@Component
public class ExecuteSettlementWriter implements ItemWriter<SettlementExecutionDto> {

    @Override
    public void write(Chunk<? extends SettlementExecutionDto> chunk) {
        log.info("정산 실행 배치 쓰기 완료 - {} 건 처리됨", chunk.size());

        for (SettlementExecutionDto dto : chunk) {
            log.debug("정산 처리 완료 - settlementId: {}, studyId: {}, 상태: {}",
                    dto.getSettlementId(),
                    dto.getStudyId(),
                    dto.settlement().getStatus());
        }
    }
}
