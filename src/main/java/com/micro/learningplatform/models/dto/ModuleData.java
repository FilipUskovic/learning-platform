package com.micro.learningplatform.models.dto;

public record ModuleData(
        String title,
        String description,
        int sequenceNumber,
        int duration
) {
}
