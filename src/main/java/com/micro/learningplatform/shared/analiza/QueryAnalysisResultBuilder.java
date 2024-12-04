package com.micro.learningplatform.shared.analiza;

import com.micro.learningplatform.shared.performace.OptimizationRecommendation;
import com.micro.learningplatform.shared.performace.PerformanceMetric;
import com.micro.learningplatform.shared.performace.QueryExecutionResult;
import com.micro.learningplatform.shared.performace.QueryPlan;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builder klasa za konstrukciju rezultata analize upita.
 * Omogućuje fleksibilnu izgradnju rezultata s različitim komponentama.
 */
public class QueryAnalysisResultBuilder {
    private List<?> results;
    private long executionTime;
    private QueryPlan queryPlan;
    private List<OptimizationRecommendation> recommendations;
    private Map<String, PerformanceMetric> performanceMetrics;

    public QueryAnalysisResultBuilder withResults(List<?> results) {
        this.results = results;
        return this;
    }

    public QueryAnalysisResultBuilder withExecutionTime(long executionTime) {
        this.executionTime = executionTime;
        return this;
    }

    public QueryAnalysisResultBuilder withQueryPlan(QueryPlan queryPlan) {
        this.queryPlan = queryPlan;
        return this;
    }

    public QueryAnalysisResultBuilder withRecommendations(
            List<OptimizationRecommendation> recommendations) {
        this.recommendations = recommendations;
        return this;
    }

    public QueryAnalysisResultBuilder withPerformanceMetrics(
            Map<String, PerformanceMetric> metrics) {
        this.performanceMetrics = metrics;
        return this;
    }

    public QueryExecutionResult build() {
        validateBuildParameters();
        return new QueryExecutionResult(
                results,
                executionTime,
                queryPlan,
                generateQueryId(),
                LocalDateTime.now(),
                recommendations
        );
    }

    private void validateBuildParameters() {
        if (results == null) {
            throw new IllegalStateException("Results cannot be null");
        }
        if (queryPlan == null) {
            throw new IllegalStateException("Query plan cannot be null");
        }
        if (executionTime <= 0) {
            throw new IllegalStateException("Execution time must be positive");
        }
    }

    private String generateQueryId() {
        return UUID.randomUUID().toString();
    }
}
