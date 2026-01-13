package com.grow.study.application.required;

import com.grow.study.application.required.dto.MemberSummaryResponse;

import java.util.List;

public interface MemberRestClient {

     MemberSummaryResponse getMemberSummary(Long userId);

     List<MemberSummaryResponse> getMemberSummaries(List<Long> memberIds);
}
