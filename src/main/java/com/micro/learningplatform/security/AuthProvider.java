package com.micro.learningplatform.security;

import lombok.Getter;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

@Getter
public enum AuthProvider {

    LOCAL("local"),
    GOOGLE("google"),
    GITHUB("github");

    private final String providerName;

    AuthProvider(String providerName) {
        this.providerName = providerName;
    }

    public static AuthProvider fromString(String provider) {
        try {
            return valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_provider"),
                    "Unsupported authentication provider: " + provider
            );
        }
    }
}
