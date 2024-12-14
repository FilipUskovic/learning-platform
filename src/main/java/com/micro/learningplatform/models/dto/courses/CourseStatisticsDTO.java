package com.micro.learningplatform.models.dto.courses;


import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

public record CourseStatisticsDTO(
        int totalModules,
        Duration totalDuration,
        Duration averageModuleDuration,
        BigDecimal completionRate,
        BigDecimal difficultyScore,
        LocalDateTime lastCalculated
) {
}
