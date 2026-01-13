package com.grow.study.adapter.intergration;

import com.grow.study.application.required.MemberRestClient;
import com.grow.study.application.required.dto.MemberBulkResponse;
import com.grow.study.application.required.dto.MemberSummaryResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class MemberRestClientAdapter implements MemberRestClient {

    private final RestClient restClient;

    public MemberRestClientAdapter(@Qualifier("loadBalancedRestClientBuilder") RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public MemberSummaryResponse getMemberSummary(Long userId) {
        return restClient.get()
                .uri("http://member-service/api/member/{id}", userId)
                .retrieve()
                .body(MemberSummaryResponse.class);
    }

    @Override
    public List<MemberSummaryResponse> getMemberSummaries(List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return List.of();
        }

        MemberBulkResponse response = restClient.post()
                .uri("http://member-service/api/member/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .body(memberIds)
                .retrieve()
                .body(MemberBulkResponse.class);

        return response != null ? response.members() : List.of();
    }
}
