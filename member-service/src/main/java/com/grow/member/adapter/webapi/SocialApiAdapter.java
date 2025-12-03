package com.grow.member.adapter.webapi;

import com.grow.member.application.member.required.GoogleUserResponse;
import com.grow.member.application.member.required.KakaoUserResponse;
import com.grow.member.application.member.required.SocialApiPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class SocialApiAdapter implements SocialApiPort {

    private final RestTemplate restTemplate;

    @Override
    public ResponseEntity<KakaoUserResponse> getKakaoUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        return restTemplate.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                KakaoUserResponse.class
        );
    }

    @Override
    public ResponseEntity<GoogleUserResponse> getGoogleUserInfo(String accessToken) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        return  restTemplate.exchange(
                "https://www.googleapis.com/oauth2/v2/userinfo",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                GoogleUserResponse.class
        );
    }
}
