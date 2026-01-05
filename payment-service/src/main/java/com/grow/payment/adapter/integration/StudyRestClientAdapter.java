package com.grow.payment.adapter.integration;

import com.grow.payment.application.dto.CompletedStudyForSettlementResponse;
import com.grow.payment.application.dto.StudySummaryResponse;
import com.grow.payment.application.required.StudyRestClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
public class StudyRestClientAdapter implements StudyRestClient {

    private final RestClient restClient;

    public StudyRestClientAdapter(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public StudySummaryResponse getMemberSummary(Long studyId) {
        return restClient.get()
                .uri("http://study-service/api/study/summary/{id}", studyId)
                .retrieve()
                .body(StudySummaryResponse.class);
    }

    /**
     * 정산 대상 스터디 목록 조회
     * Study Service의 Internal API 호출
     */
    @Override
    public List<CompletedStudyForSettlementResponse> getCompletedStudiesForSettlement(int limit) {
        log.info("정산 대상 스터디 조회 요청 - limit: {}", limit);

        List<CompletedStudyForSettlementResponse> studies = restClient.get()
                .uri("http://study-service/internal/studies/completed-for-settlement?limit={limit}", limit)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        log.info("정산 대상 스터디 {}건 조회 완료", studies != null ? studies.size() : 0);
        return studies != null ? studies : List.of();
    }

    /**
     * 스터디 정산 완료 처리
     */
    @Override
    public void markStudyAsSettled(Long studyId) {
        log.info("스터디 정산 완료 처리 요청 - studyId: {}", studyId);

        restClient.post()
                .uri("http://study-service/internal/studies/{studyId}/mark-settled", studyId)
                .retrieve()
                .toBodilessEntity();

        log.info("스터디 {} 정산 완료 처리됨", studyId);
    }
}
