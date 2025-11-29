package com.grow.apigateway.exception;


import lombok.Getter;

@Getter
public class AuthenticationException extends RuntimeException {

    private final AuthErrorCode errorCode;

    public AuthenticationException(AuthErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public AuthenticationException(AuthErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}