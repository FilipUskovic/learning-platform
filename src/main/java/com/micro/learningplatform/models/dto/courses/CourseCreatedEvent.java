package com.micro.learningplatform.models.dto.courses;

import java.util.UUID;

public record CourseCreatedEvent(
        UUID courseId
) {
}
