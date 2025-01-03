package com.micro.learningplatform.shared.exceptions;

import com.micro.learningplatform.shared.performace.QueryExecutionContext;
import lombok.Getter;

/**
 * Bazna klasa za sve iznimke vezane uz izvršavanje i analizu upita.
 * Pruža konzistentan način rukovanja greškama kroz cijeli sustav.
 */
@Getter
public class QueryAnalysisException extends RuntimeException {
    private final String queryId;
    private final QueryExecutionContext context;

    public QueryAnalysisException(String message, String queryId, QueryExecutionContext context) {
  //      super(message, cause);
        super(message);
        this.queryId = queryId;
        this.context = context;
    }



}
