package com.micro.learningplatform.models.dto.coursestatistic;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

public record CourseStatisticsSnapshotDTO(
        int totalModules,
        Duration totalDuration,
        Duration averageModuleDuration,
        BigDecimal completionRate,
        BigDecimal difficultyScore,
        LocalDateTime lastCalculated
) {
}
