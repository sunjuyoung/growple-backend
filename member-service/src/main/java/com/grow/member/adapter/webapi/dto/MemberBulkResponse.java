package com.grow.member.adapter.webapi.dto;

import com.grow.member.domain.member.Member;

import java.util.List;

public record MemberBulkResponse(
        List<MemberSummaryResponse> members
) {
    public static MemberBulkResponse of(List<Member> members) {
        return new MemberBulkResponse(
                members.stream()
                        .map(MemberSummaryResponse::of)
                        .toList()
        );
    }
}
