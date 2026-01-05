package com.grow.payment.adapter.config;

import com.grow.payment.domain.settlement.Settlement;
import com.grow.payment.domain.settlement.SettlementItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Step 1 Processor: Settlement + SettlementItem 생성
 *
 * - 스터디 정보를 받아서 Settlement 헤더 생성
 * - 각 참가자에 대해 SettlementItem 생성
 * - 출석 결과를 적용하여 환급 금액 계산
 */
@Slf4j
@Component
public class CreateSettlementProcessor implements ItemProcessor<ExpiredStudyDto, SettlementCreationResult> {

    @Override
    public SettlementCreationResult process(ExpiredStudyDto study) {
        // 참가자가 없는 스터디는 정산 생성 제외
        if (study.participants() == null || study.participants().isEmpty()) {
            log.warn("참가자가 없는 스터디 건너뜀 - studyId: {}", study.studyId());
            return null;
        }

        log.info("정산 생성 처리 - studyId: {}, title: {}, 참가자 수: {}",
                study.studyId(), study.title(), study.participants().size());

        // 1. Settlement 헤더 생성
        Settlement settlement = Settlement.create(study.studyId());

        // 2. 각 참가자에 대한 SettlementItem 생성
        List<SettlementItem> items = new ArrayList<>();

        for (ExpiredStudyDto.ParticipantDto participant : study.participants()) {
            // 보증금이 없는 참가자는 제외
            if (participant.depositPaid() == null || participant.depositPaid() <= 0) {
                log.debug("보증금 없는 참가자 제외 - participantId: {}", participant.participantId());
                continue;
            }

            SettlementItem item = SettlementItem.create(
                    null, // settlement.getId()는 저장 후 설정됨
                    participant.participantId(),
                    participant.memberId(),
                    participant.depositPaid()
            );

            // 출석 결과 적용하여 환급 금액 계산
            int absenceCount = participant.absenceCount() != null ? participant.absenceCount() : 0;
            int penaltyPerAbsence = study.penaltyPerAbsence() != null ? study.penaltyPerAbsence() : 1000;

            item.applyAttendanceResult(absenceCount, penaltyPerAbsence);

            items.add(item);

            log.debug("SettlementItem 생성 - participantId: {}, memberId: {}, " +
                            "원금: {}, 결석: {}, 페널티: {}, 환급: {}",
                    participant.participantId(),
                    participant.memberId(),
                    item.getOriginalAmount(),
                    item.getAbsenceCount(),
                    item.getPenaltyAmount(),
                    item.getRefundAmount());
        }

        if (items.isEmpty()) {
            log.warn("정산 대상 참가자가 없는 스터디 건너뜀 - studyId: {}", study.studyId());
            return null;
        }

        log.info("정산 생성 완료 - studyId: {}, 아이템 수: {}", study.studyId(), items.size());

        return SettlementCreationResult.of(settlement, items);
    }
}
