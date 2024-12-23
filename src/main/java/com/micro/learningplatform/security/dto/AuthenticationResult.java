package com.micro.learningplatform.security.dto;

import com.micro.learningplatform.models.User;

public record AuthenticationResult(
        User user,
        String message,
        boolean isSuccess,
        boolean requiresVerification
) {
}
