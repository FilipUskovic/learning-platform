package com.micro.learningplatform.models.dto.module;

import com.micro.learningplatform.models.ModuleStatus;
import com.micro.learningplatform.models.dto.DifficultyLevel;

import java.time.Duration;
import java.util.UUID;

public record ModuleBasicInfo(
        UUID id,
        String title,
        Integer sequenceNumber,
        Duration duration,
        ModuleStatus status,
        DifficultyLevel difficultyLevel
) {
}
