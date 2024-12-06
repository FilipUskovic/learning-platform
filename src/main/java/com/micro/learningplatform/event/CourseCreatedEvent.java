package com.micro.learningplatform.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record CourseCreatedEvent(
        UUID eventId,
        UUID courseId,
        LocalDateTime timestamp,
        String eventType
) implements DomainEvent {

    public CourseCreatedEvent(UUID courseId) {
        this(UUID.randomUUID(), courseId, LocalDateTime.now(), "COURSE_CREATED");
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String getEventType() {
        return eventType;
    }
}
