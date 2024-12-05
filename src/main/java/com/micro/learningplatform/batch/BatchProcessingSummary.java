package com.micro.learningplatform.batch;

import java.time.Duration;

public record BatchProcessingSummary(
        int successCount,
        int failureCount,
        Duration duration,
        double successRate,
        int errorCount
) {
}
