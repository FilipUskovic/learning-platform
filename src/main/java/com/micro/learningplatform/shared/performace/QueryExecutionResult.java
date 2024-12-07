package com.micro.learningplatform.shared.performace;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record QueryExecutionResult(
        List<?> results,
        long executionTime,
        QueryPlan queryPlan,
        String queryId,
        LocalDateTime timestamp,
        List<OptimizationRecommendation> recommendations
) {
    private static final long SLOW_QUERY_THRESHOLD = 100;


    public QueryExecutionResult {
        // Osiguravamo da imamo sve potrebne informacije
        Objects.requireNonNull(results, "Results cannot be null");
        Objects.requireNonNull(queryPlan, "Query plan cannot be null");

        // Postavljamo default vrijednosti ako nisu proslijeđene
        timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        recommendations = recommendations != null ? recommendations : new ArrayList<>();
    }


    public QueryExecutionResult(List<?> results, long executionTime, QueryPlan queryPlan) {
        this(results, executionTime, queryPlan, null, LocalDateTime.now(), List.of());
    }

    public boolean isSlowQuery() {
        return executionTime > SLOW_QUERY_THRESHOLD;
    }


    public boolean requiresOptimization() {
        return !recommendations.isEmpty() ||
                queryPlan.hasSequenceScan() ||
                queryPlan.hasNestedLoops();
    }

    public boolean shouldCache() {
        return queryPlan.estimatedRows() < 1000 &&
                !isSlowQuery() &&
                !queryPlan.isModifyingQuery();
    }


}
