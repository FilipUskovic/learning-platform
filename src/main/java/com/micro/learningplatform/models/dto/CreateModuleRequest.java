package com.micro.learningplatform.models.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Duration;

public record CreateModuleRequest(
        @NotBlank(message = "Title must not be blank")
        String title,

        String description,

        @NotNull(message = "Sequence number must not be null")
        @Positive(message = "Sequence number must be positive")
        Integer sequenceNumber,

        @NotNull(message = "Duration must not be null")
        Duration duration
) {
}
