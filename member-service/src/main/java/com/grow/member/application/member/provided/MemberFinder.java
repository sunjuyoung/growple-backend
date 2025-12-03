package com.grow.member.application.member.provided;

import com.grow.member.application.member.required.LoginResponse;
import com.grow.member.domain.Email;
import com.grow.member.domain.member.Member;

import java.util.Optional;

public interface MemberFinder {

    LoginResponse findLoginMember(String email, String password);

    Member findMember(Long memberId);

    Member findMember(String email);
}
