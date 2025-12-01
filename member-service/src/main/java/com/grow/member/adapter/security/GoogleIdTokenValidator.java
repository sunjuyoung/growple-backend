package com.grow.member.adapter.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleIdTokenValidator {

    private final GoogleOAuthProperties googleOAuthProperties;
    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    public void init() {
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance()
        )
                .setAudience(Collections.singletonList(googleOAuthProperties.getClientId()))
                .build();
    }

    /**
     * Google ID Token 검증 및 사용자 정보 추출
     */
    public GoogleUserInfo verify(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);

            if (idToken == null) {
                log.warn("Google ID Token 검증 실패: 유효하지 않은 토큰");
                throw new IllegalArgumentException("유효하지 않은 Google ID Token입니다.");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();

            return GoogleUserInfo.builder()
                    .providerId(payload.getSubject())
                    .email(payload.getEmail())
                    .emailVerified(payload.getEmailVerified())
                    .name((String) payload.get("name"))
                    .pictureUrl((String) payload.get("picture"))
                    .locale((String) payload.get("locale"))
                    .build();

        } catch (Exception e) {
            log.error("Google ID Token 검증 중 오류 발생: {}", e.getMessage());
            throw new IllegalArgumentException("Google ID Token 검증에 실패했습니다.", e);
        }
    }
}
