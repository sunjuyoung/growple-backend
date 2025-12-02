package com.grow.member.adapter.webapi.dto;

import com.grow.member.domain.member.Member;

public record MemberRegisterResponse(Long memberId, String email) {

    public static MemberRegisterResponse of(Member member) {
        return new MemberRegisterResponse(member.getId(), member.getEmail().address());
    }
}
