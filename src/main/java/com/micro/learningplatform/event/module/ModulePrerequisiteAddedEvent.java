package com.micro.learningplatform.event.module;

import java.time.LocalDateTime;
import java.util.UUID;

public record ModulePrerequisiteAddedEvent(
        UUID moduleId,
        UUID prerequisiteId,
        LocalDateTime timestamp
) implements ModuleEvent {

    public ModulePrerequisiteAddedEvent(UUID moduleId, UUID prerequisiteId) {
        this(moduleId, prerequisiteId, LocalDateTime.now());
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
