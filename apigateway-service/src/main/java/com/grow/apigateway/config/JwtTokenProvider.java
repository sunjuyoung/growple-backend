package com.grow.apigateway.config;


import com.grow.apigateway.exception.AuthErrorCode;
import com.grow.apigateway.exception.AuthenticationException;
import com.grow.apigateway.filter.TokenClaims;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLES = "roles";

    private final SecurityProperties securityProperties;
    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(
                securityProperties.getSecretKey().getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Authorization 헤더에서 토큰 추출
     */
    public String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new AuthenticationException(AuthErrorCode.TOKEN_NOT_FOUND);
        }

        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new AuthenticationException(AuthErrorCode.INVALID_TOKEN_FORMAT);
        }

        return authorizationHeader.substring(BEARER_PREFIX.length()).trim();
    }

    /**
     * 토큰 검증 및 클레임 추출
     */
    public TokenClaims validateAndGetClaims(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return TokenClaims.builder()
                    .userId(claims.get(CLAIM_USER_ID, String.class))
                    .email(claims.get(CLAIM_EMAIL, String.class))
                    .roles(extractRoles(claims))
                    .build();

        } catch (ExpiredJwtException e) {
            log.warn("토큰 만료: {}", maskToken(token));
            throw new AuthenticationException(AuthErrorCode.TOKEN_EXPIRED, e);

        } catch (SignatureException e) {
            log.warn("서명 불일치: {}", maskToken(token));
            throw new AuthenticationException(AuthErrorCode.INVALID_SIGNATURE, e);

        } catch (MalformedJwtException e) {
            log.warn("잘못된 토큰 형식: {}", maskToken(token));
            throw new AuthenticationException(AuthErrorCode.INVALID_TOKEN_FORMAT, e);

        } catch (JwtException e) {
            log.error("토큰 파싱 실패: {}", e.getMessage());
            throw new AuthenticationException(AuthErrorCode.TOKEN_PARSE_ERROR, e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Claims claims) {
        Object rolesObj = claims.get(CLAIM_ROLES);
        if (rolesObj instanceof List) {
            return (List<String>) rolesObj;
        }
        return List.of();
    }

    /**
     * 토큰 마스킹 (로그용)
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 20) {
            return "***";
        }
        return token.substring(0, 10) + "..." + token.substring(token.length() - 5);
    }
}