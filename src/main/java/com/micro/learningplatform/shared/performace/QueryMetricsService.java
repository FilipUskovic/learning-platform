package com.micro.learningplatform.shared.performace;


import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.PessimisticLockException;
import jakarta.persistence.QueryTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class QueryMetricsService {

    private final MeterRegistry meterRegistry;
    private static final String BASE_METRIC_NAME = "query";


    /**
     * Servis za praćenje metrika izvršavanja upita
    */

    public void recordQueryExecution(String queryId, QueryExecutionResult result) {
        Timer timer = Timer.builder(BASE_METRIC_NAME + ".execution")
                .tag("query", queryId)
                .tag("status", getQueryStatus(result))
                .tag("type", result.queryPlan().isModifyingQuery() ? "modifying" : "reading")
                .description("Query execution time")
                .register(meterRegistry);

        timer.record(result.executionTime(), TimeUnit.MILLISECONDS);

        // Detaljne metrike plana izvršavanja
        recordPlanMetrics(queryId, result.queryPlan());

        // Metrike performansi
        recordPerformanceMetrics(queryId, result);

        // Metrike rezultata
        recordResultMetrics(queryId, result);

        // Logiranje za monitoring
        logQueryExecution(queryId, result);
    }

    /**
     * Bilježi operacije vezane uz cache, uključujući hit/miss ratio.
     */
    public void recordCacheOperation(String queryId, boolean hit) {
        Counter counter = Counter.builder(BASE_METRIC_NAME + ".cache")
                .tag("query", queryId)
                .tag("result", hit ? "hit" : "miss")
                .description("Cache operation results")
                .register(meterRegistry);

        counter.increment();

        // Bilježimo i ukupni hit ratio
        if (hit) {
            meterRegistry.counter(BASE_METRIC_NAME + ".cache.hits").increment();
        } else {
            meterRegistry.counter(BASE_METRIC_NAME + ".cache.misses").increment();
        }
    }

    /**
     * Bilježi greške u izvršavanju upita s detaljnim informacijama.
     */
    public void recordQueryError(String queryId, Exception e) {
        Counter errorCounter = Counter.builder(BASE_METRIC_NAME + ".error")
                .tag("query", queryId)
                .tag("type", e.getClass().getSimpleName())
                .tag("message", truncateErrorMessage(e.getMessage()))
                .description("Query execution errors")
                .register(meterRegistry);

        errorCounter.increment();

        // Specifične metrike za različite tipove grešaka
        if (e instanceof QueryTimeoutException) {
            meterRegistry.counter(BASE_METRIC_NAME + ".timeout").increment();
        } else if (e instanceof PessimisticLockException) {
            meterRegistry.counter(BASE_METRIC_NAME + ".lock_conflict").increment();
        }

        // Detaljno logiranje za analizu
        log.error("Query execution error: {} - {}", queryId, e.getMessage(), e);
    }

    /**
     * Bilježi metrike vezane uz plan izvršavanja upita.
     */
    private void recordPlanMetrics(String queryId, QueryPlan plan) {
        // Procijenjeni broj redova
        meterRegistry.gauge(BASE_METRIC_NAME + ".plan.estimated_rows",
                Tags.of("query", queryId),
                plan.estimatedRows());

        // Ukupni trošak izvršavanja
        meterRegistry.gauge(BASE_METRIC_NAME + ".plan.total_cost",
                Tags.of("query", queryId),
                plan.totalCost());

        // Stvarno vrijeme izvršavanja
        meterRegistry.gauge(BASE_METRIC_NAME + ".plan.actual_time",
                Tags.of("query", queryId),
                plan.actualTime());

        // Metrike za tipove skeniranja
        recordScanTypeMetrics(queryId, plan);
    }

    /**
     * Bilježi metrike vezane uz tipove skeniranja u planu.
     */
    private void recordScanTypeMetrics(String queryId, QueryPlan plan) {
        if (plan.hasSequenceScan()) {
            meterRegistry.counter(BASE_METRIC_NAME + ".scan.sequential",
                    "query", queryId).increment();
        }

        if (plan.hasNestedLoops()) {
            meterRegistry.counter(BASE_METRIC_NAME + ".scan.nested_loops",
                    "query", queryId).increment();
        }
    }

    /**
     * Bilježi metrike vezane uz performanse izvršavanja.
     */
    private void recordPerformanceMetrics(String queryId, QueryExecutionResult result) {
        if (result.isSlowQuery()) {
            meterRegistry.counter(BASE_METRIC_NAME + ".slow",
                    "query", queryId).increment();
        }

        if (result.requiresOptimization()) {
            meterRegistry.counter(BASE_METRIC_NAME + ".needs_optimization",
                    "query", queryId).increment();

            // Bilježimo specifične preporuke za optimizaciju
            result.recommendations().forEach(rec ->
                    recordOptimizationRecommendation(queryId, rec));
        }
    }

    /**
     * Bilježi metrike vezane uz rezultate upita.
     */
    private void recordResultMetrics(String queryId, QueryExecutionResult result) {
        meterRegistry.gauge(BASE_METRIC_NAME + ".result.size",
                Tags.of("query", queryId),
                result.results().size());

        // Bilježimo i prosječnu veličinu po retku ako ima rezultata
        if (!result.results().isEmpty()) {
            double avgRowSize = calculateAverageRowSize(result.results());
            meterRegistry.gauge(BASE_METRIC_NAME + ".result.avg_row_size",
                    Tags.of("query", queryId),
                    avgRowSize);
        }
    }

    /**
     * Bilježi preporuke za optimizaciju kao metrike.
     */
    private void recordOptimizationRecommendation(String queryId,
                                                  OptimizationRecommendation recommendation) {
        meterRegistry.counter(BASE_METRIC_NAME + ".optimization",
                        "query", queryId,
                        "type", recommendation.type().name(),
                        "priority", recommendation.priority().name())
                .increment();
    }

    /**
     * Vraća status upita za označavanje metrika.
     */
    private String getQueryStatus(QueryExecutionResult result) {
        if (result.isSlowQuery()) return "slow";
        if (result.requiresOptimization()) return "needs_optimization";
        return "normal";
    }

    /**
     * Skraćuje poruku o grešci na razumnu duljinu za metrike.
     */
    private String truncateErrorMessage(String message) {
        return message != null && message.length() > 100
                ? message.substring(0, 97) + "..."
                : message;
    }

    /**
     * Izračunava prosječnu veličinu retka u rezultatima.
     */
    private double calculateAverageRowSize(List<?> results) {
        return results.stream()
                .mapToInt(row -> {
                    if (row instanceof Map) {
                        return ((Map<?, ?>) row).size();
                    }
                    return 1;
                })
                .average()
                .orElse(0.0);
    }

    /**
     * Logira detalje izvršavanja upita za potrebe monitoringa.
     */
    private void logQueryExecution(String queryId, QueryExecutionResult result) {
        if (result.isSlowQuery()) {
            log.warn("Slow query detected: {} ({}ms)", queryId, result.executionTime());
        } else {
            log.debug("Query executed: {} ({}ms)", queryId, result.executionTime());
        }

        if (result.requiresOptimization()) {
            log.info("Query optimization recommendations for {}: {}",
                    queryId,
                    formatRecommendations(result.recommendations()));
        }
    }

    /**
     * Formatira preporuke za optimizaciju u čitljiv format.
     */
    private String formatRecommendations(List<OptimizationRecommendation> recommendations) {
        return recommendations.stream()
                .map(rec -> String.format("[%s] %s (%s)",
                        rec.priority(),
                        rec.description(),
                        String.join(", ", rec.affectedObjects())))
                .collect(Collectors.joining("; "));
    }

    public void recordCacheMiss(String queryId) {
        Counter counter = Counter.builder("query.cache.miss")
                .tag("queryId", queryId)
                .description("Cache miss for query")
                .register(meterRegistry);
        counter.increment();
    }
}
