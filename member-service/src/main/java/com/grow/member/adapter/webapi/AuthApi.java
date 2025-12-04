package com.grow.member.adapter.webapi;

import com.grow.member.adapter.security.DeviceIdManager;
import com.grow.member.adapter.webapi.dto.MemberRegisterResponse;
import com.grow.member.adapter.webapi.dto.RefreshRequest;
import com.grow.member.application.member.provided.MemberAuth;
import com.grow.member.application.member.required.LoginResponse;
import com.grow.member.application.member.provided.MemberFinder;
import com.grow.member.application.member.provided.MemberRegister;
import com.grow.member.application.member.required.OAuthLogin;
import com.grow.member.application.member.required.TokenResponse;
import com.grow.member.domain.member.Member;
import com.grow.member.domain.member.MemberRegisterRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    private final DeviceIdManager deviceIdManager;
    private final MemberAuth memberAuth;

    @PostMapping("/social")
    public ResponseEntity<LoginResponse> socialLogin(
            @RequestBody SocialLoginRequest request,
            @CookieValue(name = "deviceId", required = false) String deviceId,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
            ) {

        LoginResponse response = oAuthLogin.socialLogin(request.provider, request.accessToken);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/register")
    public ResponseEntity<MemberRegisterResponse> registerMember(
            @RequestBody MemberRegisterRequest request) {

        Member member = memberRegister.register(request);

        return new ResponseEntity<>(MemberRegisterResponse.of(member), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request,
            @CookieValue(name = "deviceId", required = false) String deviceId,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
            ) {

        if (deviceId == null) {
            deviceId = deviceIdManager.generate();
            deviceIdManager.addCookie(httpResponse, deviceId);
        }

        String userAgent = httpRequest.getHeader("User-Agent");
        String ip = getClientIp(httpRequest);


        LoginResponse loginMember = memberAuth.signIn(
                request.email,
                request.password,
                deviceId,
                userAgent,
                ip
        );

        return new ResponseEntity<>(loginMember, HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestBody LogoutRequest request,
            @CookieValue(name = "deviceId") String deviceId) {

        memberAuth.logout(request.memberId, deviceId);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @RequestBody RefreshRequest request,
            @CookieValue(name = "deviceId") String deviceId,
            HttpServletRequest httpRequest) {

        String userAgent = httpRequest.getHeader("User-Agent");
        String ip = getClientIp(httpRequest);

        TokenResponse response = memberAuth.refresh(request.refreshToken(), deviceId, userAgent, ip);
        return ResponseEntity.ok(response);
    }

    public record SocialLoginRequest(String provider, String accessToken) {}

    public record LoginRequest(@Email String email, String password) {}

    public record LogoutRequest(Long memberId) {}

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
