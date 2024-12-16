package com.micro.learningplatform.models.dto.courses;

import com.micro.learningplatform.models.dto.DifficultyLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCourseRequest(

        @NotBlank(message = "Title is required")
        @Size(min = 1, max = 200)
        String title,

        String description,

        @NotNull(message = "Difficulty level is required")
        String difficultyLevel // umjesto enuma cemo korsiti string i napravili smo metodu koka vraca vrijednost enuma

) {

        public DifficultyLevel getDifficultyLevelEnum() {
                return DifficultyLevel.valueOf(difficultyLevel.toUpperCase());
        }

}
