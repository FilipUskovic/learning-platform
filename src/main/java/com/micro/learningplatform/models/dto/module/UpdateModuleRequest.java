package com.micro.learningplatform.models.dto.module;

import java.time.Duration;

public record UpdateModuleRequest(
        String title,
        String description,
        Duration duration
) {
}
