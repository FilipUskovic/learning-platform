package com.micro.learningplatform.models.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CoursePublishedEvent(
        UUID courseId,
        LocalDateTime publishedAt
) {

    public CoursePublishedEvent(UUID courseId) {
        this(courseId, LocalDateTime.now());
    }
}