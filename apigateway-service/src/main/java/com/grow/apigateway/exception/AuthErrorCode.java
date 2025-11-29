package com.grow.apigateway.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode {

    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH_001", "인증 토큰이 필요합니다"),
    INVALID_TOKEN_FORMAT(HttpStatus.UNAUTHORIZED, "AUTH_002", "잘못된 토큰 형식입니다"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_003", "토큰이 만료되었습니다"),
    INVALID_SIGNATURE(HttpStatus.UNAUTHORIZED, "AUTH_004", "유효하지 않은 토큰입니다"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_005", "접근 권한이 없습니다"),
    TOKEN_PARSE_ERROR(HttpStatus.UNAUTHORIZED, "AUTH_006", "토큰 파싱에 실패했습니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}