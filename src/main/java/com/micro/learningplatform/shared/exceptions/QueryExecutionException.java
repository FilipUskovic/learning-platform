package com.micro.learningplatform.shared.exceptions;

public class QueryExecutionException extends Throwable {
    public QueryExecutionException(String s, Exception e) {
        super(s, e);
    }
}
