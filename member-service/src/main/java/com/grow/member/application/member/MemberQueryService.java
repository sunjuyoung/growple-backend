package com.grow.member.application.member;

import com.grow.member.application.member.required.TokenResponse;
import com.grow.member.application.member.provided.MemberFinder;
import com.grow.member.application.member.required.LoginResponse;
import com.grow.member.application.member.required.MemberRepository;
import com.grow.member.application.member.required.TokenProvider;
import com.grow.member.domain.Email;
import com.grow.member.domain.member.Member;
import com.grow.member.domain.member.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberQueryService implements MemberFinder {

    private final MemberRepository memberRepository;
    private final TokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    @Override
    public LoginResponse findLoginMember(String email, String password) {

        Member member = findMember(email);

        if (!member.verifyPassword(password, passwordEncoder)) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        TokenResponse tokenResponse = tokenProvider.generateTokens(member);

        return LoginResponse.of(tokenResponse, member);
    }

    @Override
    public Member findMember(Long memberId) {
        return memberRepository.findById(memberId).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 회원입니다. ID: " + memberId));
    }

    @Override
    public Member findMember(String email) {
       return memberRepository.findByEmail(new Email(email)).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 이메일입니다. " + email));
    }
}
