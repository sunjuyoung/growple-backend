package com.grow.member.adapter.security;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GoogleUserInfo {

    private String providerId;      // Google 고유 ID (sub)
    private String email;
    private Boolean emailVerified;
    private String name;
    private String pictureUrl;
    private String locale;
}
