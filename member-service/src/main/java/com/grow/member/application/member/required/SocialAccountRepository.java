package com.grow.member.application.member.required;

import com.grow.member.domain.member.SocialAccount;
import com.grow.member.domain.member.SocialProvider;
import org.springframework.data.repository.query.Param;

import java.net.http.HttpHeaders;
import java.util.Optional;

public interface SocialAccountRepository {

    Optional<SocialAccount> findByProviderAndEmail(
            @Param("provider") SocialProvider provider,
            @Param("email") String email
    );

    boolean existsByProviderAndProviderId(SocialProvider provider, String providerId);


    SocialAccount save(SocialAccount socialAccount);

}
