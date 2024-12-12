package com.micro.learningplatform.controllers;

import com.micro.learningplatform.shared.analiza.QueryAnalysisRequest;
import com.micro.learningplatform.shared.analiza.QueryAnalysisResult;
import com.micro.learningplatform.shared.performace.CentralizedQueryAnalyzer;
import com.micro.learningplatform.shared.performace.QueryPlan;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/query")
@RequiredArgsConstructor
@Slf4j
public class QueryAnalyzerController {

    private final CentralizedQueryAnalyzer queryAnalyzer;
    private final EntityManager entityManager;

    @GetMapping("/analyze")
    public QueryAnalysisResult analyzeQuery(
            @RequestParam String query,
            @RequestParam(required = false) Map<String, String> rawParams) {
        // Pretvorba parametara u Map<String, Object>
        Map<String, Object> params = rawParams.entrySet().stream()
                .filter(entry -> !entry.getKey().equalsIgnoreCase("query")) // Ignoriraj ključ "query"
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // Validacija parametara
        validateQueryParameters(query, params);

        // Izgradnja zahtjeva za analizu
        QueryAnalysisRequest request = QueryAnalysisRequest.builder()
                .query(query)
                .parameters(params)
                .options(QueryAnalysisRequest.QueryAnalysisOptions.getDefault())
                .build();

        return queryAnalyzer.analyzeQuery(request);
    }



    @PostMapping("/analyze")
    public QueryAnalysisResult analyzeQueryPost(@RequestBody QueryAnalysisRequest queryRequest) {
        log.debug("Received query request: {}", queryRequest);

        // Modificiramo upit da koristi LIKE ili ILIKE za fleksibilniju pretragu
        String modifiedQuery = "SELECT * FROM courses WHERE title ILIKE :title";

        QueryAnalysisRequest modifiedRequest = QueryAnalysisRequest.builder()
                .query(modifiedQuery)
                .parameters(Map.of("title", "%" + queryRequest.getParameters().get("title") + "%"))
                .options(queryRequest.getOptions())
                .build();

        return queryAnalyzer.analyzeQuery(modifiedRequest);
    }

    @GetMapping("/test-basic-query")
    public List<?> testBasicQuery(@RequestParam String title) {
        Query query = entityManager.createNativeQuery("SELECT * FROM courses WHERE title = :title");
        query.setParameter("title", title);
        List<?> results = query.getResultList();
        log.debug("Results: {}", results);
        return results;
    }

    @GetMapping("/debug-analyze")
    public QueryPlan debugAnalyze(@RequestParam String query, @RequestParam Map<String, String> allParams) {
        log.debug("Received query: {}", query);
        log.debug("Received params: {}", allParams);

        // Filtriraj sve parametre osim ključa "query"
        Map<String, Object> params = new HashMap<>(allParams);
        params.remove("query");

        QueryPlan result = queryAnalyzer.analyzeQueryPlan(query, params);
        log.debug("Query plan: {}", result);

        return result;
    }

    @PostMapping("/performance/test")
    public String testPerformance(@RequestBody QueryAnalysisRequest queryRequest) {
        long startTime = System.currentTimeMillis();
        queryAnalyzer.analyzeQuery(queryRequest);
        long elapsedTime = System.currentTimeMillis() - startTime;
        return "Query analyzed in " + elapsedTime + "ms";
    }


    @ExceptionHandler(RuntimeException.class)
    public String handleExceptions(RuntimeException e) {
        return "Error during query analysis: " + e.getMessage();
    }

    private void validateQueryParameters(String query, Map<String, Object> params) {
        Pattern pattern = Pattern.compile(":[a-zA-Z_][a-zA-Z0-9_]*");
        Matcher matcher = pattern.matcher(query);

        while (matcher.find()) {
            String paramName = matcher.group().substring(1); // Uklanja dvotočku ":" posto EXPLAIN i analize nije hibernatvo ili iz jpa
            if (!params.containsKey(paramName)) {
                throw new IllegalArgumentException("Missing parameter: " + paramName);
            }
        }

        log.debug("All parameters are valid: {}", params);
    }

}
