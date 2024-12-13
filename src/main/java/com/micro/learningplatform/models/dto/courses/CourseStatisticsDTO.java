package com.micro.learningplatform.models.dto.courses;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

public record CourseStatisticsDTO(
        UUID id,
        String title,
        int totalModules,
        Duration totalDuration,
        LocalDateTime lastCalculated,
        BigDecimal completionRate,
        BigDecimal difficultyScore) {
}
