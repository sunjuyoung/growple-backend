package com.grow.member.adapter.webapi.dto;

import com.grow.member.domain.member.Member;

public record MemberSummaryResponse(
        Long id,
        String nickname,
        String profileImageUrl,
        int level
) {

    //생성
    public static MemberSummaryResponse of(Member member){
        return new MemberSummaryResponse(
                member.getId(),
                member.getNickname(),
                member.getProfileImageUrl(),
                member.getLevel().getLevel()
        );
    }
}
