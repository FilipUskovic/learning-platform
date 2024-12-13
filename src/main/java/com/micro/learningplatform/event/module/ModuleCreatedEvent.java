package com.micro.learningplatform.event.module;

import java.time.LocalDateTime;
import java.util.UUID;

public record ModuleCreatedEvent (
        UUID moduleId,
        LocalDateTime timestamp
) implements ModuleEvent {

    public ModuleCreatedEvent(UUID moduleId) {
        this(moduleId, LocalDateTime.now());
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
