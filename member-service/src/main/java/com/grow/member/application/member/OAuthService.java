package com.grow.member.application.member;

import com.grow.member.adapter.persistence.MemberJpaRepository;
import com.grow.member.adapter.persistence.SocialAccountJpaRepository;
import com.grow.member.adapter.security.*;
import com.grow.member.adapter.webapi.dto.OAuthLoginResponse;
import com.grow.member.application.member.provided.GoogleUserResponse;
import com.grow.member.application.member.provided.KakaoUserResponse;
import com.grow.member.application.member.provided.OAuthLogin;
import com.grow.member.application.member.provided.SocialUserInfo;
import com.grow.member.application.member.required.MemberRepository;
import com.grow.member.application.member.required.SocialAccountRepository;
import com.grow.member.domain.Email;
import com.grow.member.domain.member.Member;
import com.grow.member.domain.member.SocialAccount;
import com.grow.member.domain.member.SocialProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OAuthService implements OAuthLogin {

    private final GoogleIdTokenValidator googleIdTokenValidator;
    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;
    private final SocialAccountRepository socialAccountRepository;

    private final RestTemplate restTemplate;


    @Override
    public OAuthLoginResponse socialLogin(String provider, String accessToken) {

        SocialUserInfo socialUserInfo = verify(provider, accessToken);


        Member member = socialAccountRepository
                .findByProviderAndEmail(socialUserInfo.socialProvider(), socialUserInfo.email())
                .map(SocialAccount::getMember)
                .orElseGet(() -> registerNewMember(socialUserInfo));


        TokenResponse tokens = jwtTokenProvider.generateTokens(member);

        log.info("tokens: {}", tokens);

        return OAuthLoginResponse.builder()
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .expiresIn(tokens.getExpiresIn())
                .tokenType(tokens.getTokenType())
                .memberId(member.getId())
                .email(member.getEmail().address())
                .nickname(member.getNickname())
                .profileImageUrl(member.getProfileImageUrl())
                .isNewMember(false)
                .build();
    }


    public SocialUserInfo verify(String provider, String accessToken) {
        return switch (provider) {
            case "kakao" -> getKakaoUserInfo(accessToken);
            case "google" -> getGoogleUserInfo(accessToken);
            default -> throw new IllegalArgumentException("Unknown provider");
        };
    }

    private SocialUserInfo getKakaoUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<KakaoUserResponse> response = restTemplate.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                KakaoUserResponse.class
        );

        KakaoUserResponse body = response.getBody();
        log.info("Kakao User Info: {}", body);
        return new SocialUserInfo(
                body.getId().toString(),
                body.getKakaoAccount().getEmail(),
                body.getKakaoAccount().getProfile().getNickname(),
                "kakao",
                SocialProvider.KAKAO
        );
    }

    private SocialUserInfo getGoogleUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<GoogleUserResponse> response = restTemplate.exchange(
                "https://www.googleapis.com/oauth2/v2/userinfo",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                GoogleUserResponse.class
        );

        GoogleUserResponse body = response.getBody();
        log.info("Google User Info: {}", body);
        return new SocialUserInfo(
                body.getId(),
                body.getEmail(),
                body.getName(),
                "google",
                SocialProvider.GOOGLE
        );
    }




    /**
     * 신규 회원 가입 (소셜)
     */
    private Member registerNewMember(SocialUserInfo socialUserInfo) {
        // 이메일로 기존 회원 조회 (다른 소셜 계정으로 가입한 경우)
        Email email = new Email(socialUserInfo.email());

        Member member = memberRepository.findByEmail(email)
                .orElseGet(() -> createNewMember(socialUserInfo));

        // 소셜 계정 연동
        SocialAccount socialAccount = SocialAccount.of(socialUserInfo);

        socialAccountRepository.save(socialAccount);

        member.linkSocialAccount(socialAccount);

        return memberRepository.save(member);
    }

    /**
     * 새 회원 엔티티 생성
     */
    private Member createNewMember(SocialUserInfo socialUserInfo) {
        String nickname = generateUniqueNickname(socialUserInfo.name());
        String email = socialUserInfo.email();

        Member member = Member.socialRegister(email,nickname);

        return member;
    }

    /**
     * 중복되지 않는 닉네임 생성
     */
    private String generateUniqueNickname(String baseName) {
        String nickname = baseName != null ? baseName : "사용자";

        // 중복이면 랜덤 문자열 추가
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 4);
        return nickname.substring(0, Math.min(nickname.length(), 25)) + "_" + uniqueSuffix;
    }
}
