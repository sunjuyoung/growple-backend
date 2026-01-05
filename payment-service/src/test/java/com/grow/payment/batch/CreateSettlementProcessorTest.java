package com.grow.payment.batch;

import com.grow.payment.adapter.config.CreateSettlementProcessor;
import com.grow.payment.adapter.config.ExpiredStudyDto;
import com.grow.payment.adapter.config.ExpiredStudyDto.ParticipantDto;
import com.grow.payment.adapter.config.SettlementCreationResult;
import com.grow.payment.domain.settlement.Settlement;
import com.grow.payment.domain.settlement.SettlementItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CreateSettlementProcessor 단위 테스트
 */
class CreateSettlementProcessorTest {

    private CreateSettlementProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new CreateSettlementProcessor();
    }

    @Test
    @DisplayName("스터디 정보로 Settlement과 SettlementItem을 생성한다")
    void shouldCreateSettlementAndItems() throws Exception {
        // given
        ExpiredStudyDto study = new ExpiredStudyDto(
                1L,
                "테스트 스터디",
                10000,
                1000,
                List.of(
                        new ParticipantDto(100L, 1L, 10000, 2, 8),
                        new ParticipantDto(101L, 2L, 10000, 0, 10)
                )
        );

        // when
        SettlementCreationResult result = processor.process(study);

        // then
        assertThat(result).isNotNull();

        Settlement settlement = result.settlement();
        assertThat(settlement.getStudyId()).isEqualTo(1L);

        List<SettlementItem> items = result.items();
        assertThat(items).hasSize(2);
    }

    @Test
    @DisplayName("결석 횟수에 따라 환급금이 계산된다")
    void shouldCalculateRefundBasedOnAbsence() throws Exception {
        // given: 결석 3회, 페널티 1500원/회
        ExpiredStudyDto study = new ExpiredStudyDto(
                1L,
                "스터디",
                15000,
                1500,
                List.of(new ParticipantDto(100L, 1L, 15000, 3, 7))
        );

        // when
        SettlementCreationResult result = processor.process(study);

        // then: 15000 - (3 × 1500) = 10500
        SettlementItem item = result.items().get(0);
        assertThat(item.getOriginalAmount()).isEqualTo(15000);
        assertThat(item.getAbsenceCount()).isEqualTo(3);
        assertThat(item.getPenaltyAmount()).isEqualTo(4500);
        assertThat(item.getRefundAmount()).isEqualTo(10500);
    }

    @Test
    @DisplayName("참가자가 없으면 null을 반환한다")
    void shouldReturnNullWhenNoParticipants() throws Exception {
        // given
        ExpiredStudyDto study = new ExpiredStudyDto(
                1L,
                "빈 스터디",
                10000,
                1000,
                Collections.emptyList()
        );

        // when
        SettlementCreationResult result = processor.process(study);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("참가자 목록이 null이면 null을 반환한다")
    void shouldReturnNullWhenParticipantsIsNull() throws Exception {
        // given
        ExpiredStudyDto study = new ExpiredStudyDto(
                1L,
                "스터디",
                10000,
                1000,
                null
        );

        // when
        SettlementCreationResult result = processor.process(study);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("보증금이 0원인 참가자는 제외된다")
    void shouldExcludeParticipantWithZeroDeposit() throws Exception {
        // given
        ExpiredStudyDto study = new ExpiredStudyDto(
                1L,
                "스터디",
                10000,
                1000,
                List.of(
                        new ParticipantDto(100L, 1L, 10000, 0, 10),  // 보증금 있음
                        new ParticipantDto(101L, 2L, 0, 0, 10),      // 보증금 0
                        new ParticipantDto(102L, 3L, null, 0, 10)    // 보증금 null
                )
        );

        // when
        SettlementCreationResult result = processor.process(study);

        // then
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).getParticipantId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("모든 참가자가 보증금이 없으면 null을 반환한다")
    void shouldReturnNullWhenAllParticipantsHaveNoDeposit() throws Exception {
        // given
        ExpiredStudyDto study = new ExpiredStudyDto(
                1L,
                "스터디",
                10000,
                1000,
                List.of(
                        new ParticipantDto(100L, 1L, 0, 0, 10),
                        new ParticipantDto(101L, 2L, null, 0, 10)
                )
        );

        // when
        SettlementCreationResult result = processor.process(study);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("페널티가 보증금을 초과하면 환급금은 0이 된다")
    void shouldCapRefundAtZero() throws Exception {
        // given: 결석 15회, 페널티 1000원 = 15000원 페널티 > 보증금 10000원
        ExpiredStudyDto study = new ExpiredStudyDto(
                1L,
                "스터디",
                10000,
                1000,
                List.of(new ParticipantDto(100L, 1L, 10000, 15, 0))
        );

        // when
        SettlementCreationResult result = processor.process(study);

        // then
        SettlementItem item = result.items().get(0);
        assertThat(item.getPenaltyAmount()).isEqualTo(10000);  // 최대 보증금까지
        assertThat(item.getRefundAmount()).isEqualTo(0);
    }

    @Test
    @DisplayName("결석 횟수가 null이면 0으로 처리한다")
    void shouldTreatNullAbsenceAsZero() throws Exception {
        // given
        ExpiredStudyDto study = new ExpiredStudyDto(
                1L,
                "스터디",
                10000,
                1000,
                List.of(new ParticipantDto(100L, 1L, 10000, null, 10))
        );

        // when
        SettlementCreationResult result = processor.process(study);

        // then
        SettlementItem item = result.items().get(0);
        assertThat(item.getAbsenceCount()).isEqualTo(0);
        assertThat(item.getRefundAmount()).isEqualTo(10000);  // 전액 환급
    }

    @Test
    @DisplayName("페널티가 null이면 기본값 1000으로 처리한다")
    void shouldUseDefaultPenaltyWhenNull() throws Exception {
        // given
        ExpiredStudyDto study = new ExpiredStudyDto(
                1L,
                "스터디",
                10000,
                null,  // 페널티 null
                List.of(new ParticipantDto(100L, 1L, 10000, 3, 7))
        );

        // when
        SettlementCreationResult result = processor.process(study);

        // then: 10000 - (3 × 1000) = 7000
        SettlementItem item = result.items().get(0);
        assertThat(item.getPenaltyAmount()).isEqualTo(3000);
        assertThat(item.getRefundAmount()).isEqualTo(7000);
    }
}
