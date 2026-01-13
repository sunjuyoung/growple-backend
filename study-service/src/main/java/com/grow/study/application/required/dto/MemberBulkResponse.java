package com.grow.study.application.required.dto;

import java.util.List;

public record MemberBulkResponse(
        List<MemberSummaryResponse> members
) {
}
