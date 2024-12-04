package com.micro.learningplatform.shared.performace;


import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;


public record QueryExecutionResult(
        List<?> results,
        long executionTime,
        QueryPlan queryPlan,
        String queryId,
        LocalDateTime timestamp,
        List<OptimizationRecommendation> recommendations
) {
    private static final long SLOW_QUERY_THRESHOLD = 100;



    public QueryExecutionResult(List<?> results, long executionTime, QueryPlan queryPlan) {
        this(results, executionTime, queryPlan, null, LocalDateTime.now(), List.of());
    }

    public boolean isSlowQuery() {
        return executionTime > SLOW_QUERY_THRESHOLD;
    }

    public boolean requiresOptimization(){
        return !recommendations.isEmpty();
    }
}
