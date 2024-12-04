package com.micro.learningplatform.shared.performace;

import com.micro.learningplatform.shared.exceptions.QueryExecutionException;
import com.micro.learningplatform.shared.exceptions.QueryPlanParseException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class UnifiedQueryAnalyzer {

    private final EntityManager entityManager;
    private final MeterRegistry meterRegistry;
    private final CacheManager cacheManager;

    private static final String QUERY_STATS_CACHE = "queryStats";
    private static final Duration SLOW_QUERY_THRESHOLD = Duration.ofMillis(100);

    /**
     * Analiza i pracenje izvrsavanje upita
     * - PostgreSql "explain analyze" za analizu plana
     * - Metrics za pracenje performanci
     * - Kesiranje za optimizaciju cesto koristenih uptia

     */

    public QueryExecutionResult executeAndAnalyze(String query, Map<String, Object> params) throws QueryExecutionException {
        String queryId = generateQueryId(query);

        //TODO Prvo provjeravamo cache KREIRATI CHECKcACHE I recordCacheHit metode
        /*
        Optional<QueryExecutionResult> cachedResult = checkCache(queryId);
        if (cachedResult.isPresent()) {
            recordCacheHit(queryId);
            return cachedResult.get();
        }*/

        Timer.Sample timer = Timer.start(meterRegistry);
        // analiziramo plan izvrsavanja
        try {
            QueryPlan queryPlan = analyzeQueryPlan(query, params);

            // izvrsavamo upit i mjerimo performance
            Query nativeQuery = prepareQuery(query, params);
            List<?> results = nativeQuery.getResultList();

            // Mapiramo rezultate u čitljiv format
            List<Map<String, Object>> mappedResults = mapResults(results);

            // Bilježim metrike
            long executionTime = timer.stop(Timer.builder("query.execution")
                    .tag("query", query)
                    .register(meterRegistry));

            QueryExecutionResult executionResult = new QueryExecutionResult(
                    mappedResults,
                    executionTime,
                    queryPlan
            );

             // TODO dodati provjeri za dohvat iz keša
            /*
            if (shouldCache(queryPlan)) {
                cacheResult(queryId, executionResult);
            }  */

            analyzePerformance(executionResult);

            return executionResult;

        }catch (Exception | QueryPlanParseException e) {
          //  recordQueryError(queryId, e);
            assert e instanceof Exception;
            throw new QueryExecutionException("Failed to execute query: " + queryId, (Exception) e);
        }

    }

    private void analyzePerformance(QueryExecutionResult executionResult) {
        if (executionResult.executionTime() > SLOW_QUERY_THRESHOLD.toMillis()) {
            log.warn("Slow query detected: {}ms", executionResult.executionTime());

            List<OptimizationRecommendation> recommendations =
                    generateOptimizationRecommendations(executionResult);

            if (!recommendations.isEmpty()) {
                log.info("Optimization recommendations: {}", recommendations);
            }
        }

    }



    private List<OptimizationRecommendation> generateOptimizationRecommendations(QueryExecutionResult executionResult) {
        List<OptimizationRecommendation> recommendations = new ArrayList<>();
        QueryPlan plan = executionResult.queryPlan();

        if(plan.hasSequenceScan()) {
            recommendations.add(new OptimizationRecommendation(
                    OptimizationType.INDEX,
                    "Consider adding indexes to avoid sequential scans",
                    plan.getTablesWithSequentialScans(),
                    OptimizationPriority.MEDIUM
            ));
        }

            if(plan.hasNestedLoops()){
                recommendations.add(new OptimizationRecommendation(
                        OptimizationType.JOIN,
                        "Consider optimizing join conditions",
                        plan.getTablesWithNestedLoops(),
                        OptimizationPriority.MEDIUM
                ));
            }

            if(executionResult.executionTime() > SLOW_QUERY_THRESHOLD.toMillis() * 2 || executionResult.queryId() != null){
                recommendations.add(new OptimizationRecommendation(
                        OptimizationType.CACHING,
                        "Consider caching frequently accessed data",
                        Collections.singletonList(executionResult.queryId()),
                        OptimizationPriority.LOW
                ));
            }

            return recommendations;
        }



    private Query prepareQuery(String query, Map<String, Object> params) {
      //  Kreiram native query pomoću EntityManagera
        Query queryObject = entityManager.createNativeQuery(query);

        //Postavi parametre ako postoje
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                queryObject.setParameter(entry.getKey(), entry.getValue());
            }
        }
        return queryObject;
    }

    // Analizira plan izvršavanja upita koristeći PostgreSQL EXPLAIN ANALYZE
    private QueryPlan analyzeQueryPlan(String query, Map<String, Object> params) throws QueryPlanParseException {
        String explainSql = "Explain (ANALYZE, BUFFERS, FORMAT JSON) " + query;
        Query explainQuery = entityManager.createNativeQuery(explainSql);
        setQueryParameters(explainQuery, params);
        String planJson = (String) explainQuery.getSingleResult();
        return QueryPlan.fromJson(planJson);
    }

    private String generateQueryId(String query) {
        return UUID.nameUUIDFromBytes(query.getBytes(StandardCharsets.UTF_8)).toString();

    }

    private void setQueryParameters(Query query, Map<String, Object> params) {
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                query.setParameter(entry.getKey(), entry.getValue());
            }
        }
    }


    private List<Map<String, Object>> mapResults(List<?> results) {
        List<Map<String, Object>> mappedResults = new ArrayList<>();
        for (Object result : results) {
            Object[] row = (Object[]) result;
            Map<String, Object> rowMap = new HashMap<>();
            rowMap.put("createdAt", row[0]);
            rowMap.put("updatedAt", row[1]);
            rowMap.put("version", row[2]);
            rowMap.put("id", row[3]);
            rowMap.put("title", row[4]);
            rowMap.put("status", row[5]);
            rowMap.put("description", row[6]);
            mappedResults.add(rowMap);
        }
        return mappedResults;
    }

}
