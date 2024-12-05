package com.micro.learningplatform.models.dto;

public record BatchError(
        String message,
        Object data
) {
}
