package com.grow.member.adapter.security;

import com.grow.member.domain.member.Member;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

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
     * Access Token 생성
     */
    public String generateAccessToken(Member member) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());

        return Jwts.builder()
                .subject(member.getId().toString())
                .claim("userId", member.getId().toString())
                .claim("email", member.getEmail().address())
                .claim("roles", List.of(member.getRole().name()))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Refresh Token 생성
     */
    public String generateRefreshToken(Member member) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getRefreshTokenExpiration());

        return Jwts.builder()
                .subject(member.getId().toString())
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * TokenResponse 생성
     */
    public TokenResponse generateTokens(Member member) {
        return TokenResponse.builder()
                .accessToken(generateAccessToken(member))
                .refreshToken(generateRefreshToken(member))
                .expiresIn(jwtProperties.getAccessTokenExpiration() / 1000) // 초 단위
                .tokenType("Bearer")
                .build();
    }
}
