package com.grow.member.adapter.persistence;

import com.grow.member.domain.Email;
import com.grow.member.domain.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberJpaRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(Email email);

    boolean existsByEmail(Email email);

    boolean existsByNickname(String nickname);
}
