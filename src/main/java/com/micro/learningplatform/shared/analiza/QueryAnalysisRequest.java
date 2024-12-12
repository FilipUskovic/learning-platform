package com.micro.learningplatform.shared.analiza;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class QueryAnalysisRequest {
    String query;
    Map<String, Object> parameters;
    QueryAnalysisOptions options;


    @Value
    @Builder
    public static class QueryAnalysisOptions {
        boolean includeExecutionPlan;
        boolean measurePerformance;
        boolean generateRecommendations;
        boolean useCache;

        public static QueryAnalysisOptions getDefault() {
            return builder()
                    .includeExecutionPlan(true)
                    .measurePerformance(true)
                    .generateRecommendations(true)
                    .useCache(true)
                    .build();
        }
    }
}
