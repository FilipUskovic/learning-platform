package com.micro.learningplatform.models.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

public record CourseStatisticsDTO(
        UUID id,
        String title,
        int totalModules,
        Duration totalDuration,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime lastCalculated) {
}
