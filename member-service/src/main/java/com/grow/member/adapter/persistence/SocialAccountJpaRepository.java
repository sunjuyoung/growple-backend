package com.grow.member.adapter.persistence;

import com.grow.member.domain.member.SocialAccount;
import com.grow.member.domain.member.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SocialAccountJpaRepository extends JpaRepository<SocialAccount, Long> {

    @Query("SELECT sa FROM SocialAccount sa JOIN FETCH sa.member " +
            "WHERE sa.provider = :provider AND sa.email = :email")
    Optional<SocialAccount> findByProviderAndEmail(
            @Param("provider") SocialProvider provider,
            @Param("email") String email
    );

    boolean existsByProviderAndProviderId(SocialProvider provider, String providerId);
}
