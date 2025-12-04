package com.grow.member.adapter.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DeviceIdManager {

    private static final String COOKIE_NAME = "deviceId";
    private static final int MAX_AGE = 60 * 60 * 24 * 365; // 1년

    public String generate() {
        return UUID.randomUUID().toString();
    }

    public void addCookie(HttpServletResponse response, String deviceId) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, deviceId)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(MAX_AGE)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
