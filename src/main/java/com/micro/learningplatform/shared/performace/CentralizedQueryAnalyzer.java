package com.micro.learningplatform.shared.performace;

import com.micro.learningplatform.shared.analiza.QueryAnalysisContext;
import com.micro.learningplatform.shared.analiza.QueryAnalysisRequest;
import com.micro.learningplatform.shared.analiza.QueryAnalysisResult;
import com.micro.learningplatform.shared.exceptions.QueryAnalysisException;
import com.micro.learningplatform.shared.exceptions.QueryPlanParseException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class CentralizedQueryAnalyzer {

    /*
     * Centralni sustav za analizu i optimizaciju upita koji kombinira:
     * - Plan analizu
     * - Performance monitoring
     * - Caching strategije
     * - Preporuke za optimizaciju
     */

    private final EntityManager entityManager;
    private final MeterRegistry meterRegistry;
    private final CacheManager cacheManager;
    private final QueryMetricsService metricsService;


    @Value("${app.query.slow-query-threshold-ms:1000}")
    private long slowQueryThreshold;

    @Value("${app.query.cache-max-rows:1000}")
    private long cacheMaxRows;

    private static final String QUERY_STATS_CACHE = "queryResults";
    private final Map<String, List<Long>> historicalExecutionTimesCache = new ConcurrentHashMap<>();

    /*
    * -> ovo je glavni entry point za analizu upita za analizu upita
    *
     */

    public QueryAnalysisResult analyzeQuery(QueryAnalysisRequest request) {
        Timer.Sample timer = Timer.start(meterRegistry);
        String queryId = generateQueryId(request.getQuery());
        QueryExecutionContext context = createExecutionContext(request);

        try {
            if (request.getOptions().isUseCache()) {
                Optional<QueryAnalysisResult> cachedResult = checkCache(queryId);
                if (cachedResult.isPresent()) {
                    return cachedResult.get();
                }
                metricsService.recordCacheMiss(queryId);
            }

            QueryPlan queryPlan = analyzeQueryPlan(request.getQuery(), request.getParameters());
            log.debug("Generated query plan for query ID {}: {}", queryId, queryPlan);

            QueryExecutionResult executionResult = executeOptimizedQuery(request, queryPlan);

            List<OptimizationRecommendation> recommendations = generateOptimizationRecommendations(queryPlan, executionResult);
            Map<String, PerformanceMetric> metrics = collectPerformanceMetrics(executionResult, queryPlan);

            QueryAnalysisResult result = buildAnalysisResult(
                    request,
                    executionResult,
                    queryPlan,
                    recommendations,
                    metrics,
                    timer.stop(meterRegistry.timer("query.analysis.time", "query", request.getQuery()))
            );

            handleCaching(request, queryId, result);

            logQueryExecution(result);
            metricsService.recordQueryExecution(queryId, result.getExecutionResult());

            return result;
        } catch (Exception e) {
            handleAnalysisError(e, request, context);
            throw new QueryAnalysisException("Query analysis failed", queryId, context);
        }
    }

    private void handleAnalysisError(Exception e, QueryAnalysisRequest request, QueryExecutionContext context) {
        log.error("Error analyzing query [{}]: {}", request.getQuery(), e.getMessage(), e);
    }

    private void logQueryExecution(QueryAnalysisResult result) {
        if (result.getExecutionResult().isSlowQuery()) {
            log.warn("Slow query detected: {} ms", result.getExecutionResult().executionTime());
        } else {
            log.debug("Query executed: {} ms", result.getExecutionResult().executionTime());
        }
    }

    private void handleCaching(QueryAnalysisRequest request, String queryId, QueryAnalysisResult result) {
        if (request.getOptions().isUseCache()) {
            Cache queryCache = cacheManager.getCache(QUERY_STATS_CACHE);
            if (queryCache != null) {
                queryCache.put(queryId, result);
                log.debug("Cached result for query ID: {}", queryId);
            } else {
                log.warn("Cache '{}' is not available. Result not cached.", QUERY_STATS_CACHE);
            }
        }
    }

    private Map<String, PerformanceMetric> collectPerformanceMetrics(QueryExecutionResult executionResult, QueryPlan queryPlan) {
        Map<String, PerformanceMetric> metrics = new HashMap<>();

        metrics.put("execution_time", new PerformanceMetric(
                "Execution Time",
                executionResult.executionTime(),
                "ms",
                MetricType.TIMING,
                LocalDateTime.now()
        ));

        metrics.put("estimated_rows", new PerformanceMetric(
                "Estimated Rows",
                queryPlan.estimatedRows(),
                "rows",
                MetricType.SIZE,
                LocalDateTime.now()
        ));

        return metrics;
    }

    private List<OptimizationRecommendation> generateOptimizationRecommendations(QueryPlan queryPlan, QueryExecutionResult executionResult) {
        List<OptimizationRecommendation> recommendations = new ArrayList<>();

        if (queryPlan.hasSequenceScan()) {
            recommendations.add(new OptimizationRecommendation(
                    OptimizationType.INDEX,
                    "Consider adding indexes to avoid sequential scans",
                    queryPlan.getTablesWithSequentialScans(),
                    OptimizationPriority.HIGH
            ));
        }
        if (queryPlan.hasNestedLoops()) {
            recommendations.add(new OptimizationRecommendation(
                    OptimizationType.JOIN,
                    "Optimize join conditions to avoid nested loops",
                    queryPlan.getTablesWithNestedLoops(),
                    OptimizationPriority.MEDIUM
            ));
        }

        return recommendations;
    }

    private QueryExecutionResult executeOptimizedQuery(QueryAnalysisRequest request, QueryPlan queryPlan) {
        long startTime = System.nanoTime(); // Start mjerenja
        Query query = prepareOptimizedQuery(request.getQuery(), request.getParameters(), queryPlan);

        log.debug("Executing query: {}", request.getQuery());
        log.debug("Query parameters: {}", request.getParameters());

        List<?> results = query.getResultList(); // Izvršavanje query-ja
        long executionTime = System.nanoTime() - startTime; // Kraj mjerenja

        log.debug("Query returned {} results", results.size());
        log.debug("Execution time: {} ms", executionTime / 1_000_000.0);

        return new QueryExecutionResult(
                results,
                executionTime / 1_000_000, // Pretvori u milisekunde
                queryPlan,
                generateQueryId(request.getQuery()),
                LocalDateTime.now(),
                Collections.emptyList()
        );
    }

    private Query prepareOptimizedQuery(String query, Map<String, Object> parameters, QueryPlan queryPlan) {
        Query nativeQuery = entityManager.createNativeQuery(query);

        if (queryPlan.estimatedRows() > cacheMaxRows) {
            nativeQuery.setHint("org.hibernate.fetchSize", cacheMaxRows);
        }
        if (queryPlan.estimatedCost() > slowQueryThreshold) {
            nativeQuery.setHint("org.hibernate.timeout", 30);
        }

        setQueryParameters(nativeQuery, parameters);
        return nativeQuery;
    }

    private Optional<QueryAnalysisResult> checkCache(String queryId) {
        Cache cache = cacheManager.getCache(QUERY_STATS_CACHE);
        if (cache != null) {
            Cache.ValueWrapper cachedValue = cache.get(queryId);
            if (cachedValue != null) {
                return Optional.of((QueryAnalysisResult) Objects.requireNonNull(cachedValue.get()));
            }
        }
        return Optional.empty();
    }

    public QueryPlan analyzeQueryPlan(String query, Map<String, Object> params) throws QueryPlanParseException {
        try {
            String explainSql = String.format(
                    "EXPLAIN (ANALYZE true, BUFFERS true, FORMAT JSON, SETTINGS true, WAL true) %s",
                    query
            );

            Query explainQuery = entityManager.createNativeQuery(explainSql);
            setQueryParameters(explainQuery, params);

            Object result = explainQuery.getSingleResult();
            log.debug("Query Result: {}", result);

            String planJson = result instanceof Object[] ? ((Object[]) result)[0].toString() : result.toString();
            log.debug("Query Plan JSON: {}", planJson);

            return QueryPlan.fromJson(planJson);
        } catch (Exception e) {
            throw new QueryPlanParseException("Failed to execute query plan analysis.", e);
        }
    }

    private void setQueryParameters(Query query, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return;
        }

        params.forEach((key, value) -> {
            try {
                query.setParameter(key, value);
                log.debug("Setting parameter: {} = {}", key, value);
            } catch (IllegalArgumentException e) {
                log.warn("Skipping unused parameter: {}", key);
            }
        });
    }

    private String generateQueryId(String query) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(query.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.warn("Failed to generate SHA-256 hash, falling back to simple hash", e);
            return String.valueOf(query.hashCode());
        }
    }

    private QueryExecutionContext createExecutionContext(QueryAnalysisRequest request) {
        return QueryExecutionContext.builder()
                .executionTime(LocalDateTime.now())
                .parameters(request.getParameters())
                .executionEnvironment("DEFAULT")
                .additionalInfo(new HashMap<>())
                .build();
    }

    private QueryAnalysisResult buildAnalysisResult(QueryAnalysisRequest request, QueryExecutionResult executionResult, QueryPlan queryPlan, List<OptimizationRecommendation> recommendations, Map<String, PerformanceMetric> metrics, long totalAnalysisTime) {
        return QueryAnalysisResult.builder()
                .queryId(generateQueryId(request.getQuery()))
                .executionResult(executionResult)
                .queryPlan(queryPlan)
                .recommendations(recommendations)
                .metrics(metrics)
                .analysisTime(totalAnalysisTime)
                .timestamp(LocalDateTime.now())
                .context(QueryAnalysisContext.builder()
                        .requestParameters(request.getParameters())
                        .executionOptions(request.getOptions())
                        .build())
                .build();
    }
}



