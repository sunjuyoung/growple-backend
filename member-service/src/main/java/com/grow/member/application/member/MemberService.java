package com.grow.member.application.member;

import com.grow.member.application.InvalidTokenException;
import com.grow.member.application.member.provided.MemberAuth;
import com.grow.member.application.member.provided.MemberRegister;
import com.grow.member.application.member.required.*;
import com.grow.member.domain.Email;
import com.grow.member.domain.member.DuplicationEmailException;
import com.grow.member.domain.member.Member;
import com.grow.member.domain.member.MemberRegisterRequest;
import com.grow.member.domain.member.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService implements MemberRegister , MemberAuth {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberQueryService memberQueryService;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRedisRepository refreshTokenRedisRepository;

    @Override
    public Member register(MemberRegisterRequest request) {

        checkDuplicateEmail(request);

        Member member = Member.register(request,passwordEncoder);

        return memberRepository.save(member);

        //todo 이메일 인증 서비스 연동
    }


    private void checkDuplicateEmail(MemberRegisterRequest request) {
        if(memberRepository.findByEmail(new Email(request.email())).isPresent()){
            throw new DuplicationEmailException("이미 사용중인 이메일입니다."+ request.email());
        }
    }


    @Override
    public LoginResponse signIn(String email, String password, String deviceId, String userAgent, String ip) {

        Member member = memberQueryService.findMember(email);

        if (!member.verifyPassword(password, passwordEncoder)) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        TokenResponse tokenResponse = tokenProvider.generateTokens(member);

        RefreshTokenData refreshTokenData = RefreshTokenData.of(tokenResponse.getRefreshToken(), userAgent, ip);
        refreshTokenRedisRepository.save(member.getId(), deviceId, refreshTokenData);


        return LoginResponse.of(tokenResponse, member);
    }

    @Override
    public void logout(Long memberId, String deviceId) {
        refreshTokenRedisRepository.delete(memberId, deviceId);
    }

    @Override
    public TokenResponse refresh(String refreshToken, String deviceId, String userAgent, String ip) {

        // 1. 토큰에서 userId 추출
        Long userId = tokenProvider.getUserId(refreshToken);

        // 2. Redis에서 검증
        if (!refreshTokenRedisRepository.validateToken(userId, deviceId, refreshToken)) {
            // 저장된 토큰과 불일치 → 탈취 가능성
            refreshTokenRedisRepository.delete(userId, deviceId);
            throw new InvalidTokenException("토큰이 유효하지 않습니다. 다시 로그인해주세요.");
        }
        Member member = memberQueryService.findMember(userId);
        // 3. 새 토큰 발급 (Rotation)

        TokenResponse tokenResponse = tokenProvider.generateTokens(member);


        // 4. Redis 업데이트 (새 토큰으로 교체, TTL 리셋)
        RefreshTokenData newTokenData = RefreshTokenData.of(tokenResponse.getRefreshToken(), userAgent, ip);
        refreshTokenRedisRepository.save(userId, deviceId, newTokenData);

        return tokenResponse;
    }
}
