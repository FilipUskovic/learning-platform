package com.micro.learningplatform.models.dto.module;

import com.micro.learningplatform.models.ModuleStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record ModuleResponse(

        UUID id,
        String title,
        String description,
        Integer sequenceNumber,
        Duration duration,
        ModuleStatus status,
        Set<UUID> prerequisites,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

        /*
        String publicId,
        String title,
        String description,
        int sequenceNumber,
        Duration duration,
        Duration averageModuleDuration

         */
) {
}
