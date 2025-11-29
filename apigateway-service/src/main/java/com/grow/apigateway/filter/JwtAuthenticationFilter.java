package com.grow.apigateway.filter;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grow.apigateway.config.JwtTokenProvider;
import com.grow.apigateway.config.SecurityProperties;
import com.grow.apigateway.dto.ErrorResponse;
import com.grow.apigateway.exception.AuthenticationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLE = "X-User-Role";
    private static final String HEADER_USER_EMAIL = "X-User-Email";

    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityProperties securityProperties;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 1. 화이트리스트 체크
        if (securityProperties.isWhitelisted(path)) {
            log.debug("화이트리스트 경로 스킵: {}", path);
            return chain.filter(exchange);
        }

        // 2. 토큰 추출 및 검증
        try {
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            String token = jwtTokenProvider.extractToken(authHeader);
            TokenClaims claims = jwtTokenProvider.validateAndGetClaims(token);

            // 3. 검증 성공 - 사용자 정보 헤더 추가
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header(HEADER_USER_ID, claims.getUserId())
                    .header(HEADER_USER_ROLE, claims.getRolesAsString())
                    .header(HEADER_USER_EMAIL, claims.getEmail() != null ? claims.getEmail() : "")
                    .build();

            log.debug("인증 성공 - userId: {}, path: {}", claims.getUserId(), path);

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (AuthenticationException e) {
            log.warn("인증 실패 - path: {}, error: {}", path, e.getErrorCode().getCode());
            return handleAuthError(exchange, e);
        }
    }

    /**
     * 인증 에러 응답 처리
     */
    private Mono<Void> handleAuthError(ServerWebExchange exchange, AuthenticationException e) {
        ServerHttpResponse response = exchange.getResponse();

        response.setStatusCode(e.getErrorCode().getHttpStatus());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse errorResponse = ErrorResponse.of(
                e.getErrorCode().getCode(),
                e.getErrorCode().getMessage()
        );

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException ex) {
            log.error("에러 응답 직렬화 실패", ex);
            byte[] fallback = "{\"success\":false,\"error\":{\"code\":\"INTERNAL_ERROR\"}}"
                    .getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = response.bufferFactory().wrap(fallback);
            return response.writeWith(Mono.just(buffer));
        }
    }


}