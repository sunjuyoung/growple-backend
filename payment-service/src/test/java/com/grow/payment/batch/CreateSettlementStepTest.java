package com.grow.payment.batch;

import com.grow.payment.adapter.config.CreateSettlementProcessor;
import com.grow.payment.adapter.config.ExpiredStudyDto;
import com.grow.payment.adapter.config.ExpiredStudyReader;
import com.grow.payment.adapter.config.SettlementCreationResult;
import com.grow.payment.application.dto.CompletedStudyForSettlementResponse;
import com.grow.payment.application.dto.CompletedStudyForSettlementResponse.ParticipantForSettlement;
import com.grow.payment.application.required.SettlementRepository;
import com.grow.payment.application.required.StudyRestClient;
import com.grow.payment.domain.settlement.SettlementItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * Step 1: 정산 생성 Step 단위 테스트
 *
 * Spring Context 없이 Reader와 Processor 로직을 테스트합니다.
 */
@ExtendWith(MockitoExtension.class)
class CreateSettlementStepTest {

    @Mock
    private StudyRestClient studyRestClient;

    @Mock
    private SettlementRepository settlementRepository;

    private ExpiredStudyReader reader;
    private CreateSettlementProcessor processor;

    @BeforeEach
    void setUp() {
        reader = new ExpiredStudyReader(studyRestClient, settlementRepository);
        processor = new CreateSettlementProcessor();
    }

    @Test
    @DisplayName("Reader: 완료된 스터디가 있으면 ExpiredStudyDto를 반환한다")
    void readerShouldReturnExpiredStudyDtoWhenCompletedStudyExists() throws Exception {
        // given
        CompletedStudyForSettlementResponse study = createStudyResponse(
                1L, "스프링 스터디", 10000, 1000,
                List.of(
                        new ParticipantForSettlement(100L, 1L, 10000, 2, 8),
                        new ParticipantForSettlement(101L, 2L, 10000, 0, 10)
                )
        );

        when(studyRestClient.getCompletedStudiesForSettlement(anyInt()))
                .thenReturn(List.of(study));
        when(settlementRepository.findStudyIdsWithSettlement(anyList()))
                .thenReturn(Collections.emptyList());

        // when
        ExpiredStudyDto result = reader.read();

        // then
        assertThat(result).isNotNull();
        assertThat(result.studyId()).isEqualTo(1L);
        assertThat(result.participants()).hasSize(2);

        // 두 번째 호출은 null 반환 (더 이상 데이터 없음)
        assertThat(reader.read()).isNull();
    }

    @Test
    @DisplayName("Reader: 완료된 스터디가 없으면 null을 반환한다")
    void readerShouldReturnNullWhenNoCompletedStudy() throws Exception {
        // given
        when(studyRestClient.getCompletedStudiesForSettlement(anyInt()))
                .thenReturn(Collections.emptyList());

        // when
        ExpiredStudyDto result = reader.read();

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Reader: 이미 정산된 스터디는 필터링된다")
    void readerShouldFilterAlreadySettledStudy() throws Exception {
        // given
        when(studyRestClient.getCompletedStudiesForSettlement(anyInt()))
                .thenReturn(List.of(
                        createStudyResponse(1L, "스터디1", 10000, 1000,
                                List.of(new ParticipantForSettlement(100L, 1L, 10000, 0, 10))),
                        createStudyResponse(2L, "스터디2", 10000, 1000,
                                List.of(new ParticipantForSettlement(101L, 2L, 10000, 0, 10)))
                ));
        when(settlementRepository.findStudyIdsWithSettlement(anyList()))
                .thenReturn(List.of(1L));  // 스터디1은 이미 정산됨

        // when
        ExpiredStudyDto result = reader.read();

        // then: 스터디2만 반환
        assertThat(result).isNotNull();
        assertThat(result.studyId()).isEqualTo(2L);
        assertThat(reader.read()).isNull();
    }

    @Test
    @DisplayName("Processor: 스터디 정보로 Settlement과 SettlementItem을 생성한다")
    void processorShouldCreateSettlementAndItems() throws Exception {
        // given
        ExpiredStudyDto study = new ExpiredStudyDto(
                1L, "스프링 스터디", 10000, 1000,
                List.of(
                        new ExpiredStudyDto.ParticipantDto(100L, 1L, 10000, 2, 8),
                        new ExpiredStudyDto.ParticipantDto(101L, 2L, 10000, 0, 10)
                )
        );

        // when
        SettlementCreationResult result = processor.process(study);

        // then
        assertThat(result).isNotNull();
        assertThat(result.settlement().getStudyId()).isEqualTo(1L);
        assertThat(result.items()).hasSize(2);

        // 환급금 검증
        SettlementItem item1 = result.items().stream()
                .filter(i -> i.getParticipantId().equals(100L))
                .findFirst().orElseThrow();
        assertThat(item1.getRefundAmount()).isEqualTo(8000);  // 10000 - 2*1000

        SettlementItem item2 = result.items().stream()
                .filter(i -> i.getParticipantId().equals(101L))
                .findFirst().orElseThrow();
        assertThat(item2.getRefundAmount()).isEqualTo(10000);  // 개근
    }

    @Test
    @DisplayName("Processor: 참가자가 없으면 null을 반환한다")
    void processorShouldReturnNullWhenNoParticipants() throws Exception {
        // given
        ExpiredStudyDto study = new ExpiredStudyDto(
                1L, "빈 스터디", 10000, 1000, Collections.emptyList());

        // when
        SettlementCreationResult result = processor.process(study);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Processor: 보증금이 없는 참가자는 제외된다")
    void processorShouldExcludeParticipantWithNoDeposit() throws Exception {
        // given
        ExpiredStudyDto study = new ExpiredStudyDto(
                1L, "스터디", 10000, 1000,
                List.of(
                        new ExpiredStudyDto.ParticipantDto(100L, 1L, 10000, 0, 10),
                        new ExpiredStudyDto.ParticipantDto(101L, 2L, 0, 0, 10)
                )
        );

        // when
        SettlementCreationResult result = processor.process(study);

        // then
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).getParticipantId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Processor: 페널티가 보증금을 초과하면 환급금은 0이 된다")
    void processorShouldCapPenaltyAtDeposit() throws Exception {
        // given: 결석 20회 × 1000원 = 20000원 > 보증금 10000원
        ExpiredStudyDto study = new ExpiredStudyDto(
                1L, "스터디", 10000, 1000,
                List.of(new ExpiredStudyDto.ParticipantDto(100L, 1L, 10000, 20, 0))
        );

        // when
        SettlementCreationResult result = processor.process(study);

        // then
        SettlementItem item = result.items().get(0);
        assertThat(item.getPenaltyAmount()).isEqualTo(10000);  // 최대 보증금
        assertThat(item.getRefundAmount()).isEqualTo(0);
    }

    private CompletedStudyForSettlementResponse createStudyResponse(
            Long studyId, String title, Integer depositAmount, Integer penaltyPerAbsence,
            List<ParticipantForSettlement> participants
    ) {
        return new CompletedStudyForSettlementResponse(studyId, title, depositAmount, penaltyPerAbsence, participants);
    }
}
