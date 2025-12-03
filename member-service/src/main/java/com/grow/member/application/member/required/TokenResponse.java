package com.grow.member.application.member.required;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenResponse {

    private String accessToken;
    private String refreshToken;
    private long expiresIn;       // 초 단위
    private String tokenType;     // "Bearer"
}
