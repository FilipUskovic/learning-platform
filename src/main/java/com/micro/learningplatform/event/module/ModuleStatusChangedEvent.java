package com.micro.learningplatform.event.module;

import com.micro.learningplatform.models.ModuleStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ModuleStatusChangedEvent(
        UUID moduleId,
        ModuleStatus previousStatus,
        ModuleStatus newStatus,
        LocalDateTime timestamp
) implements ModuleEvent {

    public ModuleStatusChangedEvent(UUID moduleId, ModuleStatus previousStatus, ModuleStatus newStatus) {
        this(moduleId, previousStatus, newStatus, LocalDateTime.now());
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
