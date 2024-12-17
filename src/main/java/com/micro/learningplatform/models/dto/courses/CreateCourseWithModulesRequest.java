package com.micro.learningplatform.models.dto.courses;

import com.micro.learningplatform.models.dto.module.CreateModuleRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateCourseWithModulesRequest(
        @NotBlank(message = "title is required")
        String title,
        @NotBlank(message = "description is required")
        String description,
        @NotBlank(message = "Difficulty level is required")
        String difficultyLevel,
        @Valid @NotNull(message = "Modules cannot be null")
        List<CreateModuleRequest> modules
) {
}
