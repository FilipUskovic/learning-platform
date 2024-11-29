package com.micro.learningplatform.models.dto;

import com.micro.learningplatform.models.CourseStatus;

import java.time.LocalDateTime;

public record CourseResponse(
        String publicId,
        String title,
        String description,
        CourseStatus status,
        LocalDateTime createdAt
        ) {
}
