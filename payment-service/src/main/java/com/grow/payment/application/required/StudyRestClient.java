package com.grow.payment.application.required;

import com.grow.payment.application.dto.StudySummaryResponse;

public interface StudyRestClient {

     StudySummaryResponse getMemberSummary(Long userId);
}
