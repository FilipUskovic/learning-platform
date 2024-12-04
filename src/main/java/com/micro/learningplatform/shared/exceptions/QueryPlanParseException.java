package com.micro.learningplatform.shared.exceptions;

public class QueryPlanParseException extends Throwable {
    public QueryPlanParseException(String failedToParseQueryPlan, Exception e) {
        super(failedToParseQueryPlan, e);
    }
}
