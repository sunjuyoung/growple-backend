package com.grow.member.application.member.required;

import org.springframework.http.ResponseEntity;

public interface SocialApiPort {

    ResponseEntity<KakaoUserResponse> getKakaoUserInfo(String accessToken);

    ResponseEntity<GoogleUserResponse> getGoogleUserInfo(String accessToken);
}
