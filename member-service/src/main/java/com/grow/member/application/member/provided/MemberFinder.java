package com.grow.member.application.member.provided;

import com.grow.member.domain.Email;
import com.grow.member.domain.member.Member;

import java.util.Optional;

public interface MemberFinder {

    Member findLoginMember(String email, String password);
}
