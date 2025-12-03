package com.grow.member.application.member.required;

import com.grow.member.domain.member.Member;

public interface TokenProvider {

     TokenResponse generateTokens(Member member);
}
