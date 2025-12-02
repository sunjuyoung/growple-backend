package com.grow.member.application.member.required;

//  OAuth 로그인을 처리하는 메서드를 정의합니다.

public interface OAuthLogin {

    LoginResponse socialLogin(String provider, String accessToken);
}
