package com.micro.learningplatform.shared.performace;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PerformanceDistribution {
    double min;
    double max;
    double mean;
    double standardDeviation;
    long count;


    public static PerformanceDistribution empty() {
        return new PerformanceDistribution(0, 0, 0, 0, 0);
    }

    // povjera dali je distribucija stabilna
    public boolean isStable(double maxAllowedDeviation) {
        return standardDeviation <= (mean * maxAllowedDeviation);
    }

    public String createReport() {
        return String.format("""
                        Performance Distribution Report:
                        - Sample Count: %d
                        - Min: %.2f ms
                        - Max: %.2f ms
                        - Mean: %.2f ms
                        - Standard Deviation: %.2f ms
                        - Coefficient of Variation: %.2f%%
                        """,
                count,
                min,
                max,
                mean,
                standardDeviation,
                (standardDeviation / mean) * 100
        );
    }
}

