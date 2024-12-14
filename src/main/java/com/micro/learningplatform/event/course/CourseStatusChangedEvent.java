package com.micro.learningplatform.event.course;

import com.micro.learningplatform.models.CourseStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record CourseStatusChangedEvent(
        UUID courseId,
        CourseStatus previousStatus,
        CourseStatus newStatus,
        LocalDateTime timestamp
) implements CourseEvent {
    public CourseStatusChangedEvent(UUID courseId, CourseStatus previousStatus, CourseStatus newStatus) {
        this(courseId, previousStatus, newStatus, LocalDateTime.now());
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
