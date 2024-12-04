package com.micro.learningplatform.shared.analiza;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Builder
@Getter
public class PlanAnalysis {

    private final double totalCost;
    private final double actualTime;
    private final long estimatedRows;
    private final List<String> tablesInvolved;
    private final boolean hasSequentialScans;
    private final boolean hasNestedLoops;
    private final boolean isModifyingQuery;
    private final Map<String, String> scanTypes;

    /**
     * Određuje je li plan izvršavanja optimalan.
     */
    public boolean isOptimal() {
        return !hasSequentialScans &&
                !hasNestedLoops &&
                actualTime <= totalCost * 1.2; // 20% tolerancija
    }

    public List<String> getOptimizationRecommendations() {
        List<String> recommendations = new ArrayList<>();

        if (hasSequentialScans) {
            recommendations.add("Consider adding indexes for tables: " +
                    String.join(", ", getTablesWithSequentialScans()));
        }

        if (hasNestedLoops) {
            recommendations.add("Optimize join conditions to avoid nested loops");
        }

        if (actualTime > totalCost * 2) {
            recommendations.add("Query performance is significantly worse than estimated");
        }

        return recommendations;
    }

    private List<String> getTablesWithSequentialScans() {
        return scanTypes.entrySet().stream()
                .filter(e -> "Seq Scan".equals(e.getValue()))
                .map(Map.Entry::getKey)
                .toList();
    }

}
