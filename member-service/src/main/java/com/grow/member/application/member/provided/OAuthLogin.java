package com.grow.member.application.member.provided;

import com.grow.member.adapter.webapi.dto.OAuthLoginResponse;

//  OAuth 로그인을 처리하는 메서드를 정의합니다.

public interface OAuthLogin {


    OAuthLoginResponse socialLogin(String provider, String accessToken);

    //todo kakaoLogin, naverLogin 등 다른 소셜 로그인 메서드도 추가 가능
}
