package com.micro.learningplatform.batch;

import java.time.LocalDateTime;

public record BatchItemError(
        Object item,
        Exception exception,
        LocalDateTime timestamp
) {

    public BatchItemError(Object item, Exception exception) {
        this(item, exception, LocalDateTime.now());
    }
}
