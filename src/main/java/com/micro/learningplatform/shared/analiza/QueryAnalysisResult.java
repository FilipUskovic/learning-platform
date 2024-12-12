package com.micro.learningplatform.shared.analiza;

import com.micro.learningplatform.shared.performace.OptimizationRecommendation;
import com.micro.learningplatform.shared.performace.PerformanceMetric;
import com.micro.learningplatform.shared.performace.QueryExecutionResult;
import com.micro.learningplatform.shared.performace.QueryPlan;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Value
@Builder
public class QueryAnalysisResult {
    String queryId;
    QueryExecutionResult executionResult;
    QueryPlan queryPlan;
    List<OptimizationRecommendation> recommendations;
    Map<String, PerformanceMetric> metrics;
    long analysisTime;
    LocalDateTime timestamp;
    QueryAnalysisContext context;

    @Value
    @Builder
    public static class QueryAnalysisOptions {
        boolean useCache;
        boolean includeExecutionPlan;

        public static QueryAnalysisOptions defaultOptions() {
            return QueryAnalysisOptions.builder()
                    .useCache(true)
                    .includeExecutionPlan(true)
                    .build();
        }
    }


}
