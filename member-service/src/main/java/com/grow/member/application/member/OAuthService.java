package com.grow.member.application.member;

import com.grow.member.application.member.required.*;
import com.grow.member.domain.Email;
import com.grow.member.domain.member.Member;
import com.grow.member.domain.member.SocialAccount;
import com.grow.member.domain.member.SocialProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OAuthService implements OAuthLogin {

    private final TokenProvider tokenProvider;
    private final MemberRepository memberRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final SocialMemberService socialMemberService;
    private final SocialApiPort socialApiPort;


    @Override
    public LoginResponse socialLogin(String provider, String accessToken) {

        // 1. 소셜 토큰 검증 및 사용자 정보 조회
        SocialUserInfo socialUserInfo = verify(provider, accessToken);

        // 2. 기존 회원 조회 또는 신규 회원 가입
        Member member = socialMemberService.findSocialMember(socialUserInfo.socialProvider(), socialUserInfo.email())
                .map(SocialAccount::getMember)
                .orElseGet(() -> registerNewMember(socialUserInfo));

        // 3. JWT 토큰 발급
        TokenResponse tokens = tokenProvider.generateTokens(member);

        return LoginResponse.of(tokens, member);
    }


    public SocialUserInfo verify(String provider, String accessToken) {
        return switch (provider) {
            case "kakao" -> getKakaoUserInfo(accessToken);
            case "google" -> getGoogleUserInfo(accessToken);
            default -> throw new IllegalArgumentException("Unknown provider");
        };
    }

    private SocialUserInfo getKakaoUserInfo(String accessToken) {

        ResponseEntity<KakaoUserResponse> response = socialApiPort.getKakaoUserInfo(accessToken);

        KakaoUserResponse body = response.getBody();
        return new SocialUserInfo(
                body.getId().toString(),
                body.getKakaoAccount().getEmail(),
                body.getKakaoAccount().getProfile().getNickname(),
                "kakao",
                SocialProvider.KAKAO
        );
    }

    private SocialUserInfo getGoogleUserInfo(String accessToken) {

        ResponseEntity<GoogleUserResponse> response = socialApiPort.getGoogleUserInfo(accessToken);

        GoogleUserResponse body = response.getBody();
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

        Email email = new Email(socialUserInfo.email());
        // 이메일로 기존 회원 조회 (다른 소셜 계정으로 가입한 경우)
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
