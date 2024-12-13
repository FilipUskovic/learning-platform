package com.micro.learningplatform.event.course;


import java.time.LocalDateTime;
import java.util.UUID;

public record CourseCreatedEvent(
        UUID courseId,
        LocalDateTime timestamp
) implements CourseEvent {

    public CourseCreatedEvent(UUID courseId) {
        this(courseId, LocalDateTime.now());
    }


    @Override
    public UUID getCourseId() {
        return courseId;
    }


    @Override
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
