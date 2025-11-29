package com.grow.apigateway.filter;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TokenClaims {

    private final String userId;
    private final String email;
    private final List<String> roles;

    public String getRolesAsString() {
        return roles != null ? String.join(",", roles) : "";
    }
}