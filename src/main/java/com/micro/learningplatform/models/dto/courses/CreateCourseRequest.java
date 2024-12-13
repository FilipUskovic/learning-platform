package com.micro.learningplatform.models.dto.courses;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCourseRequest(
        @NotBlank (message = "Title must be provided!")
        @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters" )
        String title,
        @Size(max = 5000, message = "Description cannot exceed 5000 characters")
        String description
) {
}
