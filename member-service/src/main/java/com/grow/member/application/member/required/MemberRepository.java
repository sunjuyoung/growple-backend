package com.grow.member.application.member.required;

import com.grow.member.domain.Email;
import com.grow.member.domain.member.Member;

import java.util.Optional;

/**
 * 회원 정보를 저장하고 조회하는 기능을 제공한다.
 */
public interface MemberRepository {

    Optional<Member> findByEmail(Email email);

    boolean existsByEmail(Email email);

    boolean existsByNickname(String nickname);

    Optional<Member> findById(Long memberId);


    Member save(Member member);
}
