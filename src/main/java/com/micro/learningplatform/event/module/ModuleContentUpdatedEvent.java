package com.micro.learningplatform.event.module;

import com.micro.learningplatform.models.dto.module.ModuleData;

import java.time.LocalDateTime;
import java.util.UUID;

public record ModuleContentUpdatedEvent(
        UUID moduleId,
        ModuleData previousContent,
        ModuleData newContent,
        LocalDateTime timestamp
) implements ModuleEvent {

    public ModuleContentUpdatedEvent(UUID moduleId, ModuleData previousContent, ModuleData newContent) {
        this(moduleId, previousContent, newContent, LocalDateTime.now());
    }

    @Override
    public UUID getModuleId() {
        return moduleId;
    }

    @Override
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
