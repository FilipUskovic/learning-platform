package com.micro.learningplatform.shared.exceptions;

public class BatchProcessingInterruptedException extends Throwable {
    public BatchProcessingInterruptedException(String processingWasInterrupted) {
        super(processingWasInterrupted);
    }
}
