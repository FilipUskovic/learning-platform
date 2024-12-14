package com.micro.learningplatform.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record TestEvent(
        UUID eventId,
        String message,
        LocalDateTime timestamp
) implements DomainEvent  {


    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public LocalDateTime getTimestamp() {
        return timestamp;
    }


}
