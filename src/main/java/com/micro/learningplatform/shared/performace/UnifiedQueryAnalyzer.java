package com.micro.learningplatform.shared.performace;

import com.micro.learningplatform.shared.analiza.QueryAnalysisResultBuilder;
import com.micro.learningplatform.shared.analiza.QueryAnalysisUtils;
import com.micro.learningplatform.shared.exceptions.QueryExecutionException;
import com.micro.learningplatform.shared.exceptions.QueryPlanParseException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.jpa.AvailableHints;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class UnifiedQueryAnalyzer {

    private final EntityManager entityManager;
    private final MeterRegistry meterRegistry;
    private final CacheManager cacheManager;
    private final QueryMetricsService metricsService;


    @Value("${spring.jpa.properties.hibernate.jdbc.batch_size}")
    private int batchSize;

    @Value("${spring.jpa.properties.hibernate.query.plan_cache_max_size}")
    private int planCacheMaxSize;

    private static final String QUERY_STATS_CACHE = "queryStats";
    private static final Duration SLOW_QUERY_THRESHOLD = Duration.ofMillis(100);
    private final Map<String, List<Long>> historicalExecutionTimesCache = new ConcurrentHashMap<>();


    /**
     * Analiza i pracenje izvrsavanje upita
     * - PostgreSql "explain analyze" za analizu plana
     * - Metrics za pracenje performanci
     * - Kesiranje za optimizaciju cesto koristenih uptia

     */

    public QueryExecutionResult executeAndAnalyze(String query, Map<String, Object> params) throws QueryExecutionException {
        // Kreiramo kontekst izvršavanja koji će se koristiti kroz cijeli proces
        QueryExecutionContext context = QueryExecutionContext.builder()
                .executionTime(LocalDateTime.now())
                .parameters(params)
                .additionalInfo(new HashMap<>())
                .build();

        String queryId = generateQueryId(query);
        Timer.Sample timer = Timer.start(meterRegistry);

        try {
            // Prvo provjeravamo cache koristeći QueryExecutionContext
            Optional<QueryExecutionResult> cachedResult = checkCache(queryId, context);
            if (cachedResult.isPresent()) {
                metricsService.recordCacheOperation(queryId, true);
                return cachedResult.get();
            }

            // Analiziramo plan izvršavanja s punim kontekstom
            QueryPlan queryPlan = analyzeQueryPlan(query, params, context);

            // Izvršavamo upit i pratimo performanse
            List<?> results = executeQuery(query, params, queryPlan, context);

            // Mjerimo vrijeme izvršavanja
            long executionTime = timer.stop(Timer.builder("query.execution")
                    .tag("query", queryId)
                    .register(meterRegistry));

            // Gradimo rezultat koristeći builder pattern
            QueryExecutionResult result = new QueryAnalysisResultBuilder()
                    .withResults(results)
                    .withExecutionTime(executionTime)
                    .withQueryPlan(queryPlan)
                    .withRecommendations(generateOptimizationRecommendations(queryPlan, executionTime))
                    .withPerformanceMetrics(collectPerformanceMetrics(executionTime, queryPlan))
                    .build();

            // Analiziramo performanse i spremamo u cache ako je prikladno
            analyzeAndCacheResult(result, queryId, context);

            return result;
        } catch (QueryPlanParseException e) {
            throw new RuntimeException(e);
        }
    }

    private void analyzeAndCacheResult(
            QueryExecutionResult result,
            String queryId,
            QueryExecutionContext context) {

        // Analiziramo distribuciju vremena izvršavanja
        List<Long> executionTimes = getHistoricalExecutionTimes(queryId);
        executionTimes.add(result.executionTime());

        PerformanceDistribution distribution =
                QueryAnalysisUtils.analyzeTimeDistribution(executionTimes);

        // Detektiramo anomalije
        List<Long> anomalies = QueryAnalysisUtils.detectTimeAnomalies(
                executionTimes,
                2.0 // threshold za standardnu devijaciju
        );

        if (!anomalies.isEmpty()) {
            log.warn("Performance anomalies detected for query {}: {}",
                    queryId,
                    distribution.createReport());
        }

        // Ako je distribucija stabilna i query je pogodan za keširanje
        if (distribution.isStable(0.2) && result.shouldCache()) {
            cacheResult(queryId, result, context);
        }

        // Bilježimo sve metrike
        metricsService.recordQueryExecution(queryId, result);
    }

    private List<Long> getHistoricalExecutionTimes(String queryId) {
        return historicalExecutionTimesCache.getOrDefault(queryId, new ArrayList<>());

    }

    private void saveExecutionTime(String queryId, long executionTime) {
        // Spremanje vremena u povijest
        historicalExecutionTimesCache.computeIfAbsent(queryId, k -> new ArrayList<>()).add(executionTime);
    }


    /**
     * Demonstrira prikupljanje metrika performansi
     */
    private Map<String, PerformanceMetric> collectPerformanceMetrics(
            long executionTime,
            QueryPlan queryPlan) {

        Map<String, PerformanceMetric> metrics = new HashMap<>();

        // Osnovna metrika vremena izvršavanja
        metrics.put("execution_time", new PerformanceMetric(
                "Execution Time",
                executionTime,
                "ms",
                MetricType.TIMING,
                LocalDateTime.now()
        ));

        // Metrike plana izvršavanja
        metrics.put("estimated_rows", new PerformanceMetric(
                "Estimated Rows",
                queryPlan.estimatedRows(),
                "rows",
                MetricType.SIZE,
                LocalDateTime.now()
        ));

        // Dodajemo ostale relevantne metrike
        if (queryPlan.hasSequenceScan()) {
            metrics.put("sequential_scans", new PerformanceMetric(
                    "Sequential Scans",
                    queryPlan.getTablesWithSequentialScans().size(),
                    "count",
                    MetricType.COUNT,
                    LocalDateTime.now()
            ));
        }

        return metrics;
    }


    private List<OptimizationRecommendation> generateOptimizationRecommendations(QueryPlan plan, long executionTime) {

        List<OptimizationRecommendation> recommendations = new ArrayList<>();

        if (plan.hasSequenceScan()) {
            recommendations.add(new OptimizationRecommendation(
                    OptimizationType.INDEX,
                    "Consider adding indexes to avoid sequential scans",
                    plan.getTablesWithSequentialScans(),
                    OptimizationPriority.MEDIUM
            ));
        }

        if (plan.hasNestedLoops()) {
            recommendations.add(new OptimizationRecommendation(
                    OptimizationType.JOIN,
                    "Consider optimizing join conditions",
                    plan.getTablesWithNestedLoops(),
                    OptimizationPriority.MEDIUM
            ));
        }

        if (executionTime > SLOW_QUERY_THRESHOLD.toMillis() * 2) {
            recommendations.add(new OptimizationRecommendation(
                    OptimizationType.PERFORMANCE,
                    "Query execution time is higher than expected",
                    Collections.singletonList("Consider query optimization or caching"),
                    OptimizationPriority.HIGH
            ));
        }

        return recommendations;
    }




     // Analizira plan izvršavanja upita koristeći PostgreSQL EXPLAIN ANALYZE.

    private QueryPlan analyzeQueryPlan(String query, Map<String, Object> params, QueryExecutionContext context)
            throws QueryPlanParseException {
        String explainSql = "EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) " + query;
        Query explainQuery = entityManager.createNativeQuery(explainSql);
        setQueryParameters(explainQuery, params);
        String planJson = (String) explainQuery.getSingleResult();
        return QueryPlan.fromJson(planJson);
    }

    /**
     * Generira jedinstveni identifikator za upit.
     * Koristi SHA-256 hash za konzistentnost kroz različita izvršavanja.
     */
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


    private Optional<QueryExecutionResult> checkCache(String queryId, QueryExecutionContext context) {
        try {
            Cache queryCache = cacheManager.getCache("queryResults");
            if (queryCache != null) {
                Cache.ValueWrapper cached = queryCache.get(queryId);
                if (cached != null) {
                    metricsService.recordCacheOperation(queryId, true);
                    return Optional.ofNullable((QueryExecutionResult) cached.get());
                }
            }
            metricsService.recordCacheOperation(queryId, false);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Cache lookup failed for query {}", queryId, e);
            return Optional.empty();
        }
    }

    private List<?> executeQuery(String query, Map<String, Object> params, QueryPlan queryPlan, QueryExecutionContext context) {
        Query nativeQuery = prepareOptimizedQuery(query, params, queryPlan);
        return nativeQuery.getResultList();
    }

    private Query prepareOptimizedQuery(String query, Map<String, Object> params, QueryPlan queryPlan) {
        Query nativeQuery = entityManager.createNativeQuery(query);

        if (queryPlan.estimatedRows() > batchSize) {
            nativeQuery.setHint(AvailableHints.HINT_FETCH_SIZE, batchSize);
        }

        if (queryPlan.estimatedCost() > 1000) {
            nativeQuery.setHint(AvailableHints.HINT_TIMEOUT, 30);
        }

        if (!queryPlan.isModifyingQuery()) {
            nativeQuery.setHint(AvailableHints.HINT_CACHEABLE, true);
            nativeQuery.setHint(AvailableHints.HINT_CACHE_REGION, "query.region");
        }

        setQueryParameters(nativeQuery, params);
        return nativeQuery;
    }


    private void setQueryParameters(Query query, Map<String, Object> params) {
        if (params != null) {
            params.forEach((key, value) -> {
                if (value != null) {
                    query.setParameter(key, value);
                } else {
                    log.warn("Null parameter value for key: {}", key);
                }
            });
        }
    }


    private List<Map<String, Object>> mapResults(List<?> results) {
        List<Map<String, Object>> mappedResults = new ArrayList<>();
        for (Object result : results) {
            if (result instanceof Object[] row) {
                Map<String, Object> rowMap = new HashMap<>();
                rowMap.put("createdAt", row[0]);
                rowMap.put("updatedAt", row[1]);
                rowMap.put("version", row[2]);
                rowMap.put("id", row[3]);
                rowMap.put("title", row[4]);
                rowMap.put("status", row[5]);
                rowMap.put("description", row[6]);
                mappedResults.add(rowMap);
            } else {
                // Handling single column results
                Map<String, Object> rowMap = new HashMap<>();
                rowMap.put("value", result);
                mappedResults.add(rowMap);
            }
        }
        return mappedResults;
    }

    private void cacheResult(String queryId, QueryExecutionResult result, QueryExecutionContext context) {
        try {
            Cache queryCache = cacheManager.getCache("queryResults");
            if (queryCache != null) {
                queryCache.put(queryId, result);
                log.debug("Cached result for query: {}", queryId);
            }
        } catch (Exception e) {
            log.warn("Failed to cache result for query: {}", queryId, e);
        }
    }


}
