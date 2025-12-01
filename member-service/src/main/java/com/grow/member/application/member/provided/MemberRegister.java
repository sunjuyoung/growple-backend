package com.grow.member.application.member.provided;

import com.grow.member.domain.member.Member;
import com.grow.member.domain.member.MemberRegisterRequest;
import jakarta.validation.Valid;

public interface MemberRegister {

    Member register(@Valid MemberRegisterRequest registerRequest);

}
