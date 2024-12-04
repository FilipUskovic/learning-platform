package com.micro.learningplatform.shared.performace;

import java.time.LocalDateTime;

public record PerformanceMetric(
        String name,
        double value,
        String unit,
        MetricType type,
        LocalDateTime timestamp
) {


    public String format() {
        return String.format("%s: %.2f %s", name, value, unit);
    }

    public boolean exceedsThreshold(double threshold) {
        return value > threshold;
    }
}
