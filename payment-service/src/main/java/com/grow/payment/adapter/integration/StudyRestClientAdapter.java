package com.grow.payment.adapter.integration;

import com.grow.payment.application.dto.StudySummaryResponse;
import com.grow.payment.application.required.StudyRestClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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
}
