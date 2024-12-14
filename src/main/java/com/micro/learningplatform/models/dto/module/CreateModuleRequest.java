package com.micro.learningplatform.models.dto.module;

import com.micro.learningplatform.models.dto.DifficultyLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;
import java.util.Set;

public record CreateModuleRequest(

        @NotBlank(message = "Title is required")
        String title,

        String description,

       // @NotNull(message = "Sequence number is required")
        Integer sequenceNumber,

        @Min(value = 1, message = "Sequence number must be positive")
        Long durationInMinutes,

        // isto opcionalno
        Set<String> prerequisiteIds,

        // opcionalno jer ako ga ne postavim sam, dohvati ce od course tezinu
        DifficultyLevel difficultyLevel // Ovo je opcionalno

) {
        public Duration getDuration() {
                return Duration.ofMinutes(durationInMinutes);
        }
}
