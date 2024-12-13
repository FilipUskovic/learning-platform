package com.micro.learningplatform.models.dto.courses;

import com.micro.learningplatform.models.dto.DifficultyLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateCourseRequest(
        @NotBlank(message = "Title is required")
        @Size(min = 1, max = 200)
        String title,

        String description,

        @NotNull(message = "Difficulty level is required")
        String difficultyLevel
      //  DifficultyLevel difficultyLevel
) {

        public DifficultyLevel getDifficultyLevelEnum() {
                return DifficultyLevel.valueOf(difficultyLevel);
        }
}
