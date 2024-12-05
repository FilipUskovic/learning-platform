package com.micro.learningplatform.shared.exceptions;

public class BatchProcessingException extends Throwable {
    public BatchProcessingException(String batchProcessingFailed, Exception e) {
        super(batchProcessingFailed, e);
    }
}
