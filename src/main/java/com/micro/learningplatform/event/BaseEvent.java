package com.micro.learningplatform.event;

import java.time.LocalDateTime;
import java.util.UUID;

public abstract class BaseEvent {
    private final UUID eventId;
    private final LocalDateTime timestamp;

    protected BaseEvent() {
        this.eventId = UUID.randomUUID();
        this.timestamp = LocalDateTime.now();
    }

    public UUID getEventId() {
        return eventId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
