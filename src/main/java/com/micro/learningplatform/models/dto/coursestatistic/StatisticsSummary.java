package com.micro.learningplatform.models.dto.coursestatistic;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

public record StatisticsSummary(
        int totalModules,
        Duration totalDuration,
        Duration averageModuleDuration,
        BigDecimal completionRate,
        BigDecimal difficultyScore,
        LocalDateTime lastCalculated
) {
    public String getFormattedCompletionRate() {
        return completionRate.multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP) + "%";
    }

    public String getFormattedDuration() {
        return DurationFormatter.format(totalDuration);
    }

    public static final class DurationFormatter {
        private DurationFormatter() {
        }

        public static String format(Duration duration) {
            long hours = duration.toHours();
            long minutes = duration.toMinutesPart();

            if (hours > 0) {
                return String.format("%dh %dm", hours, minutes);
            }
            return String.format("%dm", minutes);
        }
    }
}
