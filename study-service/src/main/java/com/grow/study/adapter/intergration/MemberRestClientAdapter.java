package com.grow.study.adapter.intergration;

import com.grow.study.application.required.MemberRestClient;
import com.grow.study.application.required.dto.MemberSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MemberRestClientAdapter implements MemberRestClient {

    private final RestClient restClient;


    public MemberRestClientAdapter(@Qualifier("loadBalancedRestClientBuilder")RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public MemberSummaryResponse getMemberSummary(Long userId) {
       return restClient.get()
                .uri("http://member-service/api/member/{id}", userId)
                .retrieve()
                .body(MemberSummaryResponse.class);

    }
}
