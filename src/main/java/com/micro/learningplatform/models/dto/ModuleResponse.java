package com.micro.learningplatform.models.dto;

public record ModuleResponse(
        String publicId,
        String title,
        String description,
        int sequenceNumber,
        int duration
) {
}
