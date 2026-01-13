package com.grow.member.application.member.provided;

import com.grow.member.domain.member.Member;

import java.util.List;

public interface MemberFinder {

    Member findMember(Long memberId);

    Member findMember(String email);

    List<Member> findMembers(List<Long> memberIds);

}
