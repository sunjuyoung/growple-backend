package com.grow.member.adapter.webapi;

import com.grow.member.adapter.webapi.dto.MemberRegisterResponse;
import com.grow.member.application.member.required.LoginResponse;
import com.grow.member.application.member.provided.MemberFinder;
import com.grow.member.application.member.provided.MemberRegister;
import com.grow.member.application.member.required.OAuthLogin;
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

    @PostMapping("/social")
    public ResponseEntity<LoginResponse> socialLogin(
            @RequestBody SocialLoginRequest request) {

        LoginResponse response = oAuthLogin.socialLogin(request.provider, request.accessToken);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/register")
    public ResponseEntity<MemberRegisterResponse> registerMember(
            @RequestBody MemberRegisterRequest request) {

        log.info(request.toString());
        Member member = memberRegister.register(request);

        return new ResponseEntity<>(MemberRegisterResponse.of(member), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request) {

        LoginResponse loginMember = memberFinder.findLoginMember(request.email, request.password);

        return new ResponseEntity<>(loginMember, HttpStatus.OK);
    }


    public record SocialLoginRequest(String provider, String accessToken) {}

    public record LoginRequest(@Email String email, String password) {}
}
