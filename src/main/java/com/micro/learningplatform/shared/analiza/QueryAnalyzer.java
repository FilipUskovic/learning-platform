package com.micro.learningplatform.shared.analiza;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class QueryAnalyzer {

    private final EntityManager entityManager;

    public String explainQuery(String query, Map<String, Object> parameters) {
        String explainSql = "EXPLAIN ANALYZE " + query;
        Query nativeQuery = entityManager.createNativeQuery(explainSql);

        // Postavljamo parametre
        parameters.forEach(nativeQuery::setParameter);

        // Dohvaćamo i analiziramo plan
        List<String> results = nativeQuery.getResultList();
        return analyzePlan(planLines);

    }

    private String analyzePlan(List<String> planLines) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("Query Plan Analysis:\n");

        for (String line : planLines) {
            if (line.contains("Seq Scan")) {
                analysis.append("WARNING: Sequential scan detected - consider adding index\n");
            }
            if (line.contains("cost=")) {
                analysis.append("Performance metrics found: ").append(line).append("\n");
            }
        }

        return analysis.toString();
    }

}
