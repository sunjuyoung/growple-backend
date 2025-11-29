package com.grow.apigateway.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class SecurityProperties {

    private String secretKey;
    private String issuer;
    private List<String> whitelistPaths = new ArrayList<>();

    // 화이트리스트 경로 패턴 매칭
    public boolean isWhitelisted(String path) {
        return whitelistPaths.stream()
                .anyMatch(pattern -> matchPath(pattern, path));
    }

    private boolean matchPath(String pattern, String path) {
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix);
        }
        return pattern.equals(path);
    }
}