package com.grow.payment.application.required;

import com.grow.payment.application.dto.StudySummaryResponse;
import com.grow.payment.application.dto.CompletedStudyForSettlementResponse;

import java.util.List;

public interface StudyRestClient {

    StudySummaryResponse getMemberSummary(Long userId);

    /**
     * 정산 대상 스터디 목록 조회
     * COMPLETED 상태의 스터디 + 참가자 정보
     */
    List<CompletedStudyForSettlementResponse> getCompletedStudiesForSettlement(int limit);

    /**
     * 스터디 정산 완료 처리
     */
    void markStudyAsSettled(Long studyId);
}
