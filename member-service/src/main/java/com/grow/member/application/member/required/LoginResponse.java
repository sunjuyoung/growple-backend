package com.grow.member.application.member.required;

import com.grow.member.domain.member.Member;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {

    // JWT 토큰 정보
    private String accessToken;
    private String refreshToken;
    private long expiresIn;


    // 회원 정보
    private Long memberId;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private int point;
    private int level;
    private String levelTitle;

    // 신규 회원 여부
    private boolean isNewMember;



    public static LoginResponse of(TokenResponse response, Member member) {
        return LoginResponse.builder()
                .accessToken(response.getAccessToken())
                .refreshToken(response.getRefreshToken())
                .expiresIn(response.getExpiresIn())
                .memberId(member.getId())
                .email(member.getEmail().address())
                .nickname(member.getNickname())
                .profileImageUrl(member.getProfileImageUrl())
                .point(member.getPoint())
                .level(member.getLevel().getLevel())
                .levelTitle(member.getLevel().getTitle())
                .isNewMember(false)
                .build();
    }

}
