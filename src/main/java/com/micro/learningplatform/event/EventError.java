package com.micro.learningplatform.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventError(
        Throwable cause,
        UUID eventId,
        LocalDateTime timestamp
) {
    public EventError(Throwable cause, UUID eventId) {
        this(cause, eventId, LocalDateTime.now());
    }

    public EventError(Exception e) {
        this(e, UUID.randomUUID());
    }
}
