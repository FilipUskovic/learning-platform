package com.micro.learningplatform.models.dto.courses;

import com.micro.learningplatform.models.dto.module.CreateModuleRequest;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateCourseWithModulesRequest(
        @NotBlank(message = "title is required")
        String title,
        @NotBlank(message = "description is required")
        String description,
        @NotBlank(message = "Difficulty level is required")
        String difficultyLevel,
        List<CreateModuleRequest> modules
) {
}
