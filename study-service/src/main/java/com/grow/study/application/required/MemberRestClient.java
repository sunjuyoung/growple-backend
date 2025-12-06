package com.grow.study.application.required;

import com.grow.study.application.required.dto.MemberSummaryResponse;

public interface MemberRestClient {

     MemberSummaryResponse getMemberSummary(Long userId);
}
