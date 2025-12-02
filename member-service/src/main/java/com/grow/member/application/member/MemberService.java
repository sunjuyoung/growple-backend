package com.grow.member.application.member;

import com.grow.member.application.member.provided.MemberRegister;
import com.grow.member.application.member.required.MemberRepository;
import com.grow.member.domain.Email;
import com.grow.member.domain.member.DuplicationEmailException;
import com.grow.member.domain.member.Member;
import com.grow.member.domain.member.MemberRegisterRequest;
import com.grow.member.domain.member.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService implements MemberRegister {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Member register(MemberRegisterRequest request) {

        checkDuplicateEmail(request);

        Member member = Member.register(request,passwordEncoder);

        Member saved = memberRepository.save(member);

        //todo 이메일 인증 서비스 연동

        return saved;
    }

    private void checkDuplicateEmail(MemberRegisterRequest request) {
        if(memberRepository.findByEmail(new Email(request.email())).isPresent()){
            throw new DuplicationEmailException("이미 사용중인 이메일입니다."+ request.email());
        }
    }
}
