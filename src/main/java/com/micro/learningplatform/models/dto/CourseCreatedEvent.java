package com.micro.learningplatform.models.dto;

import java.util.UUID;

public record CourseCreatedEvent(
        UUID courseId
) {
}
