package com.micro.learningplatform.controllers;

import com.micro.learningplatform.models.dto.QueryRequest;
import com.micro.learningplatform.shared.exceptions.QueryExecutionException;
import com.micro.learningplatform.shared.performace.QueryExecutionResult;
import com.micro.learningplatform.shared.performace.UnifiedQueryAnalyzer;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/query")
@RequiredArgsConstructor
public class QueryAnalyzerController {

    private final UnifiedQueryAnalyzer queryAnalyzer;

    @GetMapping("/analyze")
    public QueryExecutionResult analyzeQuery(@RequestParam String query, @RequestParam Map<String, Object> params) throws QueryExecutionException {
        return queryAnalyzer.executeAndAnalyze(query, params != null ? params : Map.of());
    }

    @PostMapping("/analyze")
    public QueryExecutionResult analyzeQueryPost(@RequestBody QueryRequest queryRequest) {
        try {
            return queryAnalyzer.executeAndAnalyze(queryRequest.query(), queryRequest.params());
        } catch (Exception | QueryExecutionException e) {
            e.printStackTrace();
            throw new RuntimeException("Query execution failed: " + e.getMessage());
        }
    }

}
