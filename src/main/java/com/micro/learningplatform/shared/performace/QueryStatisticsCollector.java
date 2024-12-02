package com.micro.learningplatform.shared.performace;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class QueryStatisticsCollector {

  /* Analiza trentova u performancsama i automatsko upozoravanje na degradaciju performansi

   */

    private static final Map<String, List<QueryExecution>> queryStatistics =
            new ConcurrentHashMap<>();

    public static void recordQueryExecution(String queryName, long duration, LocalDateTime executionTime){
        queryStatistics.computeIfAbsent(queryName, k -> new ArrayList<>())
                .add(new QueryExecution(duration, executionTime));

        // Čistimo stare statistike
        cleanOldStatistics();

        // Analiziramo trend
        analyzeTrend(queryName);
    }

    private static void analyzeTrend(String queryName) {
        List<QueryExecution> executions = queryStatistics.get(queryName);
        if (executions.size() < 10) return; // Trebamo više podataka za analizu

        // Računamo prosječno vrijeme izvršavanja za zadnjih 10 izvršavanja
        double avgTime = executions.stream()
                .skip(executions.size()- 10)
                .mapToLong(QueryExecution::duration)
                .average()
                .orElse(0.0);

        // Ako je prosječno vrijeme iznad praga, šaljemo upozorenje
            if(avgTime > 1000) { // više od 1 sekunde
                log.warn("Performance degradation detected for query: {}. " +
                        "Average execution time: {} ms", queryName, avgTime);
            }
    }

    private static void cleanOldStatistics() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        queryStatistics.values().forEach(executions ->
            executions.removeIf(execution ->
                    execution.executionTime().isBefore(threshold))
        );
    }



    }
