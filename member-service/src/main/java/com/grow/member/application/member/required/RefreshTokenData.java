package com.grow.member.application.member.required;

import java.time.LocalDateTime;

public record RefreshTokenData(
        String token,           // refresh token 값
        String userAgent,       // 브라우저/기기 정보
        String ip,              // 접속 IP
        LocalDateTime createdAt // 생성 시간
) {
    public static RefreshTokenData of(String token, String userAgent, String ip) {
        return new RefreshTokenData(
                token,
                userAgent,
                ip,
                LocalDateTime.now()
        );
    }
}