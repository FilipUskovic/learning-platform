package com.micro.learningplatform.shared.analiza;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class QueryAnalysisContext {
    Map<String, Object> requestParameters;
    QueryAnalysisRequest.QueryAnalysisOptions executionOptions;
}
