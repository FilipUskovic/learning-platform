package com.micro.learningplatform.models.dto.module;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

public record CreateModuleRequest(

        @NotBlank(message = "Title is required")
        String title,

        String description,

        @NotNull(message = "Sequence number is required")
        Integer sequenceNumber,

        @NotNull(message = "Duration is required")
        Long durationInMinutes,

        Set<String> prerequisiteIds
      //  Set<UUID> prerequisites // biti ce string ids da izbejegnemo opterenjce i cuircule reference

       /*
        @NotBlank(message = "Title must not be blank")
        String title,

        String description,

        @NotNull(message = "Sequence number must not be null")
        @Positive(message = "Sequence number must be positive")
        Integer sequenceNumber,

        @NotNull(message = "Duration must not be null")
        Duration duration

        */
) {
        public Duration getDuration() {
                return Duration.ofMinutes(durationInMinutes);
        }
}
