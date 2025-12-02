package com.grow.member.application.member;

import com.grow.member.application.member.provided.SocialMemberFinder;
import com.grow.member.application.member.required.SocialAccountRepository;
import com.grow.member.domain.member.SocialAccount;
import com.grow.member.domain.member.SocialProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SocialMemberService implements SocialMemberFinder {

    private final SocialAccountRepository socialAccountRepository;

    @Override
    public Optional<SocialAccount> findSocialMember(SocialProvider socialProvider, String email) {

        return socialAccountRepository.findByProviderAndEmail(socialProvider, email);
    }
}
