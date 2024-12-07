package com.micro.learningplatform.models.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CourseResponseWithModules(
        String publicId,
        String title,
        String description,
        String status,
        LocalDateTime createdAt,
        List<ModuleResponse> modules
) {
}
