package com.grow.payment.batch;

import com.grow.payment.adapter.batch.ExecuteSettlementProcessor;
import com.grow.payment.adapter.batch.SettlementExecutionDto;
import com.grow.payment.adapter.batch.SettlementReader;
import com.grow.payment.application.required.MemberRestClient;
import com.grow.payment.application.required.SettlementItemRepository;
import com.grow.payment.application.required.SettlementRepository;
import com.grow.payment.application.required.StudyRestClient;
import com.grow.payment.domain.settlement.Settlement;
import com.grow.payment.domain.settlement.SettlementItem;
import com.grow.payment.domain.settlement.SettlementItemStatus;
import com.grow.payment.domain.settlement.SettlementStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Step 2: 정산 실행 Step 단위 테스트
 *
 * Spring Context 없이 Reader와 Processor 로직을 테스트합니다.
 */
@ExtendWith(MockitoExtension.class)
class ExecuteSettlementStepTest {

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private SettlementItemRepository settlementItemRepository;

    @Mock
    private MemberRestClient memberRestClient;

    @Mock
    private StudyRestClient studyRestClient;

    private SettlementReader reader;
    private ExecuteSettlementProcessor processor;

    @BeforeEach
    void setUp() {
        reader = new SettlementReader(settlementRepository, settlementItemRepository);
        processor = new ExecuteSettlementProcessor(memberRestClient, studyRestClient, settlementRepository, settlementItemRepository);
    }

    @Test
    @DisplayName("Reader: PENDING 상태의 Settlement을 반환한다")
    void readerShouldReturnPendingSettlement() throws Exception {
        // given
        Settlement settlement = createSettlement(1L, 100L);
        SettlementItem item = createSettlementItem(1L, 100L, 1L, 10000, 0, 10000);

        when(settlementRepository.findSettlementsToProcess(any()))
                .thenReturn(List.of(settlement));
        when(settlementItemRepository.findItemsToProcess(eq(1L), any()))
                .thenReturn(List.of(item));

        // when
        SettlementExecutionDto result = reader.read();

        // then
        assertThat(result).isNotNull();
        assertThat(result.getSettlementId()).isEqualTo(1L);
        assertThat(result.itemsToProcess()).hasSize(1);

        assertThat(reader.read()).isNull();
    }

    @Test
    @DisplayName("Reader: 처리할 Settlement이 없으면 null을 반환한다")
    void readerShouldReturnNullWhenNoSettlement() throws Exception {
        // given
        when(settlementRepository.findSettlementsToProcess(any()))
                .thenReturn(Collections.emptyList());

        // when
        SettlementExecutionDto result = reader.read();

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Processor: 포인트 지급 성공 시 Item을 PAYOUT_DONE으로 변경한다")
    void processorShouldMarkItemAsPaidOnSuccess() throws Exception {
        // given
        Settlement settlement = createSettlement(1L, 100L);
        SettlementItem item = createSettlementItem(1L, 100L, 1L, 10000, 0, 10000);
        SettlementExecutionDto dto = SettlementExecutionDto.of(settlement, List.of(item));

        when(memberRestClient.addPoints(eq(1L), eq(10000L), anyString()))
                .thenReturn(999L);
        when(settlementItemRepository.areAllItemsCompleted(1L))
                .thenReturn(true);

        // when
        processor.process(dto);

        // then
        assertThat(item.getStatus()).isEqualTo(SettlementItemStatus.PAYOUT_DONE);
        assertThat(item.getProcessedPointTxId()).isEqualTo(999L);
        verify(memberRestClient).addPoints(eq(1L), eq(10000L), anyString());
    }

    @Test
    @DisplayName("Processor: 환급금이 0원이면 포인트 지급 없이 완료 처리된다")
    void processorShouldCompleteWithoutPayoutWhenRefundIsZero() throws Exception {
        // given
        Settlement settlement = createSettlement(1L, 100L);
        SettlementItem item = createSettlementItem(1L, 100L, 1L, 10000, 10, 0);  // 환급금 0
        SettlementExecutionDto dto = SettlementExecutionDto.of(settlement, List.of(item));

        when(settlementItemRepository.areAllItemsCompleted(1L))
                .thenReturn(true);

        // when
        processor.process(dto);

        // then
        assertThat(item.getStatus()).isEqualTo(SettlementItemStatus.PAYOUT_DONE);
        verify(memberRestClient, never()).addPoints(anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Processor: 포인트 지급 실패 시 Item을 FAILED로 변경하고 재시도를 스케줄링한다")
    void processorShouldMarkItemAsFailedOnError() throws Exception {
        // given
        Settlement settlement = createSettlement(1L, 100L);
        SettlementItem item = createSettlementItem(1L, 100L, 1L, 10000, 0, 10000);
        SettlementExecutionDto dto = SettlementExecutionDto.of(settlement, List.of(item));

        when(memberRestClient.addPoints(anyLong(), anyLong(), anyString()))
                .thenThrow(new RuntimeException("Member Service 연결 실패"));
        when(settlementItemRepository.areAllItemsCompleted(1L))
                .thenReturn(false);

        // when
        processor.process(dto);

        // then
        assertThat(item.getStatus()).isEqualTo(SettlementItemStatus.FAILED);
        assertThat(item.getRetryCount()).isEqualTo(1);
        assertThat(item.getNextRetryAt()).isNotNull();
        assertThat(item.getLastError()).contains("Member Service 연결 실패");
    }

    @Test
    @DisplayName("Processor: 모든 Item 완료 시 Settlement을 COMPLETED로 변경한다")
    void processorShouldCompleteSettlementWhenAllItemsDone() throws Exception {
        // given
        Settlement settlement = createSettlement(1L, 100L);
        SettlementItem item = createSettlementItem(1L, 100L, 1L, 10000, 0, 10000);
        SettlementExecutionDto dto = SettlementExecutionDto.of(settlement, List.of(item));

        when(memberRestClient.addPoints(anyLong(), anyLong(), anyString()))
                .thenReturn(1L);
        when(settlementItemRepository.areAllItemsCompleted(1L))
                .thenReturn(true);

        // when
        processor.process(dto);

        // then
        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.COMPLETED);
        verify(studyRestClient).markStudyAsSettled(100L);
    }

    @Test
    @DisplayName("Processor: 여러 참가자에게 순차적으로 포인트가 지급된다")
    void processorShouldPayoutToMultipleParticipants() throws Exception {
        // given
        Settlement settlement = createSettlement(1L, 100L);
        SettlementItem item1 = createSettlementItem(1L, 100L, 1L, 10000, 0, 10000);
        SettlementItem item2 = createSettlementItem(1L, 101L, 2L, 10000, 2, 8000);
        SettlementItem item3 = createSettlementItem(1L, 102L, 3L, 10000, 5, 5000);
        SettlementExecutionDto dto = SettlementExecutionDto.of(settlement, List.of(item1, item2, item3));

        when(memberRestClient.addPoints(anyLong(), anyLong(), anyString()))
                .thenReturn(1L);
        when(settlementItemRepository.areAllItemsCompleted(1L))
                .thenReturn(true);

        // when
        processor.process(dto);

        // then
        verify(memberRestClient).addPoints(eq(1L), eq(10000L), anyString());
        verify(memberRestClient).addPoints(eq(2L), eq(8000L), anyString());
        verify(memberRestClient).addPoints(eq(3L), eq(5000L), anyString());

        assertThat(item1.getStatus()).isEqualTo(SettlementItemStatus.PAYOUT_DONE);
        assertThat(item2.getStatus()).isEqualTo(SettlementItemStatus.PAYOUT_DONE);
        assertThat(item3.getStatus()).isEqualTo(SettlementItemStatus.PAYOUT_DONE);
    }

    // 헬퍼 메서드
    private Settlement createSettlement(Long id, Long studyId) {
        Settlement settlement = Settlement.create(studyId);
        setField(settlement, "id", id);
        return settlement;
    }

    private SettlementItem createSettlementItem(
            Long settlementId, Long participantId, Long memberId,
            long originalAmount, int absenceCount, long refundAmount
    ) {
        SettlementItem item = SettlementItem.create(settlementId, participantId, memberId, originalAmount);
        int penaltyPerAbsence = absenceCount > 0 ? (int) ((originalAmount - refundAmount) / absenceCount) : 0;
        item.applyAttendanceResult(absenceCount, penaltyPerAbsence);
        setField(item, "settlementId", settlementId);
        return item;
    }

    private void setField(Object obj, String fieldName, Object value) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            // AbstractEntity에 있는 경우
            try {
                Field field = obj.getClass().getSuperclass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(obj, value);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
