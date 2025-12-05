package com.grow.study.adapter.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(
                jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8)
        );
    }


    /**
     * JWT 토큰에서 사용자 ID 추출
     */
    public Long getUserId(String token) {

        Claims claims = Jwts.parser()
                .verifyWith(secretKey)      // 0.12.x에서는 verifyWith() 사용
                .build()
                .parseSignedClaims(token)   // parseClaimsJws() → parseSignedClaims()
                .getPayload();

        return Long.parseLong(claims.get("userId").toString());
    }

    /**
     * Authorization 헤더에서 토큰 추출
     */
    public String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization 헤더가 올바르지 않습니다.");
        }
        return authorizationHeader.substring(7);
    }
}
