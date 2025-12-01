package com.grow.member.adapter.webapi;

import com.grow.member.adapter.webapi.dto.MemberRegisterResponse;
import com.grow.member.adapter.webapi.dto.OAuthLoginResponse;
import com.grow.member.application.member.OAuthService;
import com.grow.member.application.member.provided.MemberFinder;
import com.grow.member.application.member.provided.MemberRegister;
import com.grow.member.application.member.provided.OAuthLogin;
import com.grow.member.domain.member.Member;
import com.grow.member.domain.member.MemberRegisterRequest;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApi {

    private final OAuthLogin oAuthLogin;
    private final MemberRegister memberRegister;
    private final MemberFinder memberFinder;

    /**
     * Google 소셜 로그인
     * - Next.js에서 Google OAuth 완료 후 id_token을 전달받음
     * - id_token 검증 후 자체 JWT 발급
     */
    @PostMapping("/social")
    public ResponseEntity<OAuthLoginResponse> socialLogin(
            @RequestBody SocialLoginRequest request
    ) {
        OAuthLoginResponse response = oAuthLogin.socialLogin(request.provider, request.accessToken);
        log.info("socialLogin response: {}", response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<MemberRegisterResponse> registerMember(
            @RequestBody MemberRegisterRequest request
    ) {
        Member member = memberRegister.register(request);

        return new ResponseEntity<>(MemberRegisterResponse.of(member), HttpStatus.OK);
    }

    @PostMapping("/login")
    public ResponseEntity<MemberRegisterResponse> login(
            @RequestBody LoginRequest request
    ) {
        memberFinder.findLoginMember(request.email, request.password);

        return null;
    }



    public record SocialLoginRequest(String provider, String accessToken) {}

    public record LoginRequest(@Email String email, String password) {}
}
