package com.micro.learningplatform.models.dto.module;

import com.micro.learningplatform.models.ModuleStatus;

import java.util.UUID;

public record ModuleReferenceInfo(
        UUID id,
        String title,
        ModuleStatus status
) {
}
