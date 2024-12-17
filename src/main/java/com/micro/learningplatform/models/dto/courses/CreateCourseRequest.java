package com.micro.learningplatform.models.dto.courses;

import com.micro.learningplatform.models.dto.DifficultyLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


public record CreateCourseRequest(

        @NotBlank(message = "Title is required")
        @Size(min = 1, max = 200)
        String title,

        String description,

        @NotNull(message = "Difficulty level is required")
        @Pattern(regexp = "BEGINNER|INTERMEDIATE|ADVANCED", message = "Difficulty level must be BEGINNER, INTERMEDIATE, or ADVANCED")
        String difficultyLevel // umjesto enuma cemo korsiti string i napravili smo metodu koka vraca vrijednost enuma

) {

        public DifficultyLevel getDifficultyLevelEnum() {
                return DifficultyLevel.valueOf(difficultyLevel.toUpperCase());
        }

}
