package com.grow.member.application.member.provided;

import com.grow.member.domain.member.SocialAccount;
import com.grow.member.domain.member.SocialProvider;

import java.util.Optional;

public interface SocialMemberFinder {

    Optional<SocialAccount> findSocialMember(SocialProvider socialProvider, String email);

}
