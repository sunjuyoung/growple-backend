package com.grow.member.application.member;

import com.grow.member.application.member.provided.MemberFinder;
import com.grow.member.application.member.required.MemberRepository;
import com.grow.member.domain.Email;
import com.grow.member.domain.member.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberQueryService implements MemberFinder {

    private final MemberRepository memberRepository;

    @Override
    public Member findLoginMember(String email, String password) {

        Member member = memberRepository.findByEmail(new Email(email)).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다. " + email));



        return null;
    }
}
