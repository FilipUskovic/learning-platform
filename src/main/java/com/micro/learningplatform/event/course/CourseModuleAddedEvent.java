package com.micro.learningplatform.event.course;

import java.time.LocalDateTime;
import java.util.UUID;

public record CourseModuleAddedEvent(
        UUID courseId,
        UUID moduleId,
        LocalDateTime timestamp
) implements CourseEvent {

    public CourseModuleAddedEvent(UUID courseId, UUID moduleId) {
        this(courseId, moduleId, LocalDateTime.now());
    }

    @Override
    public UUID getCourseId() {
        return courseId;
    }


    @Override
    public UUID getEventId() {
        return null;
    }

    @Override
    public LocalDateTime getTimestamp() {
        return timestamp;
    }


}
