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
        //  DifficultyLevel difficultyLevel


      //  @NotNull(message = "Author ID is required")
     //  UUID authorId // Novo polje za validaciju autora
        /*
        @NotBlank (message = "Title must be provided!")
        @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters" )
        String title,
        @Size(max = 5000, message = "Description cannot exceed 5000 characters")
        String description

         */
) {

        public DifficultyLevel getDifficultyLevelEnum() {
                return DifficultyLevel.valueOf(difficultyLevel.toUpperCase());
        }

}
