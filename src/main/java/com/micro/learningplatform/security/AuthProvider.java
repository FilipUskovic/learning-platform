package com.micro.learningplatform.security;

import lombok.Getter;

@Getter
public enum AuthProvider {

    LOCAL("local"),
    GOOGLE("google"),
    GITHUB("github");

    private final String providerName;

    AuthProvider(String providerName) {
        this.providerName = providerName;
    }
}
