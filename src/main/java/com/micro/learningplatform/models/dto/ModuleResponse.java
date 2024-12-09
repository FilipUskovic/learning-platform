package com.micro.learningplatform.models.dto;

import java.time.Duration;

public record ModuleResponse(
        String publicId,
        String title,
        String description,
        int sequenceNumber,
        Duration duration,
        Duration averageModuleDuration
) {
}
