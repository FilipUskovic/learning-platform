package com.micro.learningplatform.models.dto;

import com.micro.learningplatform.models.CourseStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record CourseResponse(
        UUID Id,
        String title,
        String description,
        CourseStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
