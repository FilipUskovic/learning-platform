package com.micro.learningplatform.shared.analiza;

import com.micro.learningplatform.shared.performace.PerformanceDistribution;
import lombok.experimental.UtilityClass;

import java.util.DoubleSummaryStatistics;
import java.util.List;

@UtilityClass
public class QueryAnalysisUtils {

    public static PerformanceDistribution analyzeTimeDistribution(
            List<Long> executionTimes) {
        if (executionTimes.isEmpty()) {
            return PerformanceDistribution.empty();
        }

        DoubleSummaryStatistics stats = executionTimes.stream()
                .mapToDouble(Long::doubleValue)
                .summaryStatistics();

        return PerformanceDistribution.builder()
                .min(stats.getMin())
                .max(stats.getMax())
                .mean(stats.getAverage())
                .count(stats.getCount())
                .standardDeviation(calculateStandardDeviation(executionTimes,
                        stats.getAverage()))
                .build();
    }

    private static double calculateStandardDeviation(List<Long> values,
                                                     double mean) {
        double variance = values.stream()
                .mapToDouble(Long::doubleValue)
                .map(value -> Math.pow(value - mean, 2))
                .average()
                .orElse(0.0);

        return Math.sqrt(variance);
    }

    /**
     * Detektira anomalije u vremenima izvršavanja.
     */
    public static List<Long> detectTimeAnomalies(List<Long> executionTimes,
                                                 double threshold) {
        PerformanceDistribution distribution = analyzeTimeDistribution(
                executionTimes);
        double upperBound = distribution.getMean() +
                (threshold * distribution.getStandardDeviation());

        return executionTimes.stream()
                .filter(time -> time > upperBound)
                .toList();
    }
}
