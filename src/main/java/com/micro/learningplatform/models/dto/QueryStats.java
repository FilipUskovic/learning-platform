package com.micro.learningplatform.models.dto;

public record QueryStats(
        String query,
        long executionCount,
        double executionAvgTime,
        long executionMaxTime,
        long executionMinTime,
        long executionRowCount,
        long cacheHitCount,
        long cacheMissCount
) {
}
