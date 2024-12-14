package com.micro.learningplatform.models.dto.module;

import com.micro.learningplatform.models.ModuleStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record ModuleDetailResponse(
        UUID id,
        String title,
        String description,
        Integer sequenceNumber,
        Duration duration,
        ModuleStatus status,
        // Samo ID kursa, ne cijeli objekt
        UUID courseId,
        // Pojednostavljeni prikaz prerequisita
        Set<ModuleReferenceInfo> prerequisites,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
