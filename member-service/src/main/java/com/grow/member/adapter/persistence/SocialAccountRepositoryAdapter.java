package com.grow.member.adapter.persistence;

import com.grow.member.application.member.required.SocialAccountRepository;
import com.grow.member.domain.member.SocialAccount;
import com.grow.member.domain.member.SocialProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SocialAccountRepositoryAdapter implements SocialAccountRepository {

    private final SocialAccountJpaRepository socialAccountJpaRepository;



    @Override
    public Optional<SocialAccount> findByProviderAndEmail(SocialProvider provider, String email) {
        return socialAccountJpaRepository.findByProviderAndEmail(provider, email);
    }

    @Override
    public boolean existsByProviderAndProviderId(SocialProvider provider, String providerId) {
        return socialAccountJpaRepository.existsByProviderAndProviderId(provider, providerId);
    }

    @Override
    public SocialAccount save(SocialAccount socialAccount) {
        return socialAccountJpaRepository.save(socialAccount);
    }
}
