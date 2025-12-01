package com.grow.member.adapter.webapi.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OAuthLoginResponse {

    // JWT 토큰 정보
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private String tokenType;

    // 회원 정보
    private Long memberId;
    private String email;
    private String nickname;
    private String profileImageUrl;

    // 신규 회원 여부
    private boolean isNewMember;
}
