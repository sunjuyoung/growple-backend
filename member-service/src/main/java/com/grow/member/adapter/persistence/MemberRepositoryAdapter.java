package com.grow.member.adapter.persistence;

import com.grow.member.application.member.required.MemberRepository;
import com.grow.member.domain.Email;
import com.grow.member.domain.member.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberRepositoryAdapter implements MemberRepository{

    private final  MemberJpaRepository memberJpaRepository;

    @Override
    public Optional<Member> findByEmail(Email email) {
        return memberJpaRepository.findByEmail(email);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return memberJpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByNickname(String nickname) {
        return memberJpaRepository.existsByNickname(nickname);
    }

    @Override
    public Optional<Member> findById(Long memberId) {
        return memberJpaRepository.findById(memberId);
    }

    @Override
    public Member save(Member member) {
        return memberJpaRepository.save(member);
    }
}
