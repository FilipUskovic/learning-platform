package com.micro.learningplatform.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record CoursePublishedEvent(
        UUID eventId,
        UUID courseId,
        LocalDateTime timestamp,
        String eventType
) implements DomainEvent {

    public CoursePublishedEvent(UUID courseId) {
        this(UUID.randomUUID(), courseId, LocalDateTime.now(), "COURSE_PUBLISHED");
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
