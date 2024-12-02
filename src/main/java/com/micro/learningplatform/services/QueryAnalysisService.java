package com.micro.learningplatform.services;



import com.micro.learningplatform.models.dto.QueryStats;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.stat.QueryStatistics;
import org.hibernate.stat.Statistics;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class QueryAnalysisService {

    /* Analiza performansi upita s preporukama za optimizaciju i Statistike za donošenje odluka o optimizaciji

     */

    private final EntityManager entityManager;

    public List<QueryStats> analyzeQueries() {
        // Dohvaćamo statistike iz Hibernate Statistics
        Statistics stats = entityManager.unwrap(Session.class)
                .getSessionFactory()
                .getStatistics();

        List<QueryStats> queryStatsList = new ArrayList<>();

        // analizirmo
        for (String query : stats.getQueries()) {
            QueryStatistics queryStat = stats.getQueryStatistics(query);

            QueryStats queryStats = new QueryStats(
                    query,
                    queryStat.getExecutionCount(),
                    queryStat.getExecutionAvgTime(), // Prosječno vrijeme izvršavanja
                    queryStat.getExecutionMaxTime(),
                    queryStat.getExecutionMinTime(),
                    queryStat.getExecutionRowCount(),
                    queryStat.getCacheHitCount(),
                    queryStat.getCacheMissCount()
            );

            // Dodavanje preporuka za optimizaciju
            addOptimizationRecommendations(queryStats);
            queryStatsList.add(queryStats);
        }

        return queryStatsList;
    }


    private void addOptimizationRecommendations(QueryStats queryStats) {
        if (queryStats.executionAvgTime() > 1000) {
            log.info("Razmotrite dodavanje indeksa za poboljšanje performansi.");
        }
        if (queryStats.cacheMissCount() > 0 && queryStats.cacheHitCount() == 0) {
            log.info("Omogućite keširanje upita kako biste smanjili opterećenje baze podataka.");
        }
    }

}
